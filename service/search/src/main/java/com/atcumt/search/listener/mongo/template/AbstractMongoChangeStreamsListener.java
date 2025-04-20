package com.atcumt.search.listener.mongo.template;

import com.atcumt.common.utils.RedisLockUtil;
import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.FullDocumentBeforeChange;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class AbstractMongoChangeStreamsListener {
    protected MongoTemplate mongoTemplate;
    protected RedisTemplate<String, String> redisStringTemplate;
    protected RedissonClient redissonClient;
    private RedisLockUtil redisLockUtil;
    private RLock lock;
    private final ScheduledExecutorService listeningScheduler = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService renewalScheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService restartExecutor = Executors.newSingleThreadExecutor();
    private volatile ScheduledFuture<?> listeningFuture;
    private volatile ScheduledFuture<?> renewalFuture;
    protected volatile MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor;
    protected volatile boolean locked = false;
    private final AtomicBoolean isRestarting = new AtomicBoolean(false);
    private volatile long requestId;

    protected final String COLLECTION;
    private final String RELEASE_LOCK_KEY;
    private final String LOCK_KEY;

    private static final long TRY_LOCK_TIME = 5;  // 获取锁超时时间
    private static final long TRY_LOCK_INTERVAL = 5;  // 获取锁间隔
    private static final long LOCK_EXPIRE_TIME = 30;  // 锁过期时间
    private static final long LOCK_RENEW_INTERVAL = LOCK_EXPIRE_TIME / 3;  // 锁延时间隔
    private static final long RELEASE_LOCK_EXPIRE_TIME = 300;  // 释放锁过期时间

    protected AbstractMongoChangeStreamsListener(
            MongoTemplate mongoTemplate,
            RedisTemplate<String, String> redisStringTemplate,
            RedissonClient redissonClient,
            String collection
    ) {
        this.mongoTemplate = mongoTemplate;
        this.redisStringTemplate = redisStringTemplate;
        this.redissonClient = redissonClient;

        this.COLLECTION = collection;
        this.RELEASE_LOCK_KEY = "mongo:change_streams_release_lock:" + collection;
        this.LOCK_KEY = "mongo:change_streams_lock:" + collection;
    }

    @PostConstruct
    public void startListening() {
        this.redisLockUtil = RedisLockUtil.create(redisStringTemplate, LOCK_EXPIRE_TIME, TimeUnit.SECONDS);
        this.lock = redissonClient.getLock(LOCK_KEY);

        tryToAcquireLockAndListen();
    }

    private void tryToAcquireLockAndListen() {
        listeningFuture = listeningScheduler.scheduleWithFixedDelay(() -> {
            requestId = Thread.currentThread().threadId();

            try {
                if (lock.tryLock(TRY_LOCK_TIME, LOCK_EXPIRE_TIME, TimeUnit.SECONDS)) {
                    log.info("[{}] 🔓 获取到 Redis 锁，开始监听 MongoDB Change Streams", COLLECTION);
                    locked = true;
                    startManualLockRenewal();
                    try {
                        watchMongoChangeStreams();
                    } catch (Exception e) {
                        if (locked) {
                            log.error("[{}] MongoDB Change Streams 监听异常，准备重启 | {}", COLLECTION, e.getLocalizedMessage());
                            stopListeningAndRestart();
                        }
                    }
                    try {
                        if (lock.isLocked() && lock.isHeldByThread(requestId)) {
                            lock.unlock();
                        }
                    } catch (Exception e) {
                        if (locked) log.error("[{}] Redis 锁释放异常，准备重启 | {}", COLLECTION, e.getLocalizedMessage());
                    } finally {
                        locked = false;
                    }
                }
//                else {
//                    log.info("[{}] 🔒 未获取到 Redis 锁，等待下次尝试", COLLECTION);
//                }
            } catch (Exception e) {
                log.error("[{}] Redis 锁获取 & MongoDB Change Streams 监听异常，准备重启 | ", COLLECTION, e);
            }
        }, TRY_LOCK_INTERVAL, TRY_LOCK_INTERVAL, TimeUnit.SECONDS);
    }

    private void stopListeningAndRestart() {
        restartExecutor.submit(() -> {
            if (!isRestarting.compareAndSet(false, true)) {
                return;
            }

            locked = false;

            try {
                if (renewalFuture != null) {
                    renewalFuture.cancel(true);
                }

                if (listeningFuture != null) {
                    listeningFuture.cancel(true);
                }

                closeMongoCursor();

                log.info("[{}] 🔄 重新启动监听", COLLECTION);
                tryToAcquireLockAndListen();
            } finally {
                isRestarting.set(false);
            }
        });
    }

    // 0 0 4 * * *
    @Scheduled(cron = "0 0 4 * * *")
    private void releaseLock() {
        if (redisLockUtil.tryLock(RELEASE_LOCK_KEY, RELEASE_LOCK_EXPIRE_TIME, TimeUnit.SECONDS)) {
            log.info("[{}] 🔓 定时任务释放 Redis 锁", COLLECTION);
            lock.forceUnlock();
        }
    }

    private void startManualLockRenewal() {
        renewalFuture = renewalScheduler.scheduleWithFixedDelay(() -> {
            if (!locked || lock == null) {
                log.info("[{}] 🔒 Redis 锁已失效，停止续期", COLLECTION);
                return;
            }

            if (!lock.isHeldByThread(requestId)) {
                log.warn("[{}] 🔒 Redis 锁已被其他线程获取，停止续期并重新获取", COLLECTION);
                stopListeningAndRestart();
                return;
            }

            try {
                boolean renewed = redisLockUtil.forceRenewLock(LOCK_KEY);
                if (!renewed) {
                    log.warn("[{}] 🔒 续期失败，重新获取 Redis 锁", COLLECTION);
                    stopListeningAndRestart();
                }
            } catch (Exception e) {
                log.error("[{}] 🔒 续期异常", COLLECTION, e);
                stopListeningAndRestart();
            }
        }, LOCK_RENEW_INTERVAL, LOCK_RENEW_INTERVAL, TimeUnit.SECONDS);
    }

    private synchronized void closeMongoCursor() {
        if (cursor != null) {
            try {
                cursor.close();
            } catch (Exception e) {
                log.error("[{}] 关闭 MongoDB Change Streams 失败", COLLECTION, e);
            } finally {
                cursor = null;
            }
        }
    }

    public void setCursor(List<Bson> pipeline) {
        setCursor(pipeline, FullDocument.DEFAULT, FullDocumentBeforeChange.OFF);
    }

    public void setCursor(List<Bson> pipeline, FullDocument fullDocument, FullDocumentBeforeChange fullDocumentBeforeChange) {
        String stringResumeToken = redisStringTemplate.opsForValue().get("mongo:change_streams_resume_token:" + COLLECTION);
        String stringOperationTime = redisStringTemplate.opsForValue().get("mongo:change_streams_operation_time:" + COLLECTION);

        var preCursor = mongoTemplate
                .getCollection(COLLECTION)
                .watch(pipeline)
                .fullDocument(fullDocument)
                .fullDocumentBeforeChange(fullDocumentBeforeChange);

        if (stringResumeToken != null) {
            BsonDocument resumeToken = BsonDocument.parse(stringResumeToken);
            try {
                cursor = preCursor.resumeAfter(resumeToken).cursor();
            } catch (Exception e) {
                log.warn("[{}] 🔍 ResumeToken 解析失败", COLLECTION);
                BsonTimestamp operationTime = new BsonTimestamp((int) (System.currentTimeMillis() / 1000 - 60 * 60 * 24), 0); // 1天前
                if (stringOperationTime != null) operationTime = new BsonTimestamp(Long.parseLong(stringOperationTime));
                preCursor = mongoTemplate
                        .getCollection(COLLECTION)
                        .watch(pipeline)
                        .fullDocument(fullDocument)
                        .fullDocumentBeforeChange(fullDocumentBeforeChange);
                cursor = preCursor.startAtOperationTime(operationTime).cursor();
            }
        } else {
            BsonTimestamp operationTime = new BsonTimestamp((int) (System.currentTimeMillis() / 1000 - 60 * 60 * 24), 0); // 1天前
            cursor = preCursor.startAtOperationTime(operationTime).cursor();
        }
    }

    public ChangeStreamDocument<Document> getNext() {
        ChangeStreamDocument<Document> next = cursor.next();
        redisStringTemplate.opsForValue().set("mongo:change_streams_resume_token:" + COLLECTION, next.getResumeToken().toJson());
        // 记录操作时间
        BsonTimestamp operationTime = new BsonTimestamp((int) (System.currentTimeMillis() / 1000), 0);
        if (next.getClusterTime() != null) {
            operationTime = next.getClusterTime();
        }
        redisStringTemplate.opsForValue().set("mongo:change_streams_operation_time:" + COLLECTION, String.valueOf(operationTime.getValue()));

        return next;
    }

    public abstract void watchMongoChangeStreams();
}