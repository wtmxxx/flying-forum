package com.atcumt.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisLockUtil {
    private final RedisTemplate<String, String> redisTemplate;
    private long LOCK_EXPIRE_TIME;
    private TimeUnit LOCK_EXPIRE_TIME_UNIT;

    private RedisLockUtil(RedisTemplate<String, String> redisTemplate, long expireTime, TimeUnit expireTimeUnit) {
        this.redisTemplate = redisTemplate;
        this.LOCK_EXPIRE_TIME = expireTime;
        this.LOCK_EXPIRE_TIME_UNIT = expireTimeUnit;
    }

    public static RedisLockUtil create(RedisTemplate<String, String> redisTemplate, long expireTime, TimeUnit expireTimeUnit) {
        return new RedisLockUtil(redisTemplate, expireTime, expireTimeUnit);
    }

    public static RedisLockUtil create(RedisTemplate<String, String> redisTemplate) {
        return new RedisLockUtil(redisTemplate, 30, TimeUnit.SECONDS);
    }

    public void setLockExpireTime(long expireTime, TimeUnit expireTimeUnit) {
        this.LOCK_EXPIRE_TIME = expireTime;
        this.LOCK_EXPIRE_TIME_UNIT = expireTimeUnit;
    }

    /**
     * 尝试获取锁
     * @param lockKey 锁的 Key
     * @param requestId 请求标识（用于区分不同线程）
     * @param expireTime 过期时间（秒）
     * @return 是否加锁成功
     */
    public boolean acquireLock(String lockKey, String requestId, long expireTime, TimeUnit expireTimeUnit) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, requestId, expireTime, expireTimeUnit);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 尝试获取锁（默认过期时间）
     * @param lockKey 锁的 Key
     * @param requestId 请求标识
     * @return 是否加锁成功
     */
    public boolean acquireLock(String lockKey, String requestId) {
        return acquireLock(lockKey, requestId, LOCK_EXPIRE_TIME, LOCK_EXPIRE_TIME_UNIT);
    }
    
    /**
     * 尝试获取锁（默认请求标识）
     * @param lockKey 锁的 Key
     * @param expireTime 过期时间（秒）
     * @return 是否加锁成功
     */
    public boolean tryLock(String lockKey, long expireTime, TimeUnit expireTimeUnit) {
        return acquireLock(lockKey, "1", expireTime, expireTimeUnit);
    }

    public boolean tryLock(String lockKey) {
        return acquireLock(lockKey, "1", LOCK_EXPIRE_TIME, LOCK_EXPIRE_TIME_UNIT);
    }

    /**
     * 续期锁（需在业务代码中定期调用）
     * @param lockKey 锁的 Key
     * @param requestId 请求标识
     * @return 是否续期成功
     */
    public boolean renewLock(String lockKey, String requestId, long expireTime) {
        String value = redisTemplate.opsForValue().get(lockKey);
        if (requestId.equals(value)) {
            return Boolean.TRUE.equals(redisTemplate.expire(lockKey, expireTime, LOCK_EXPIRE_TIME_UNIT));
        }
        return false;
    }

    /**
     * 续期锁（默认过期时间）
     * @param lockKey 锁的 Key
     * @param requestId 请求标识
     * @return 是否续期成功
     */
    public boolean renewLock(String lockKey, String requestId) {
        return renewLock(lockKey, requestId, LOCK_EXPIRE_TIME);
    }

    /**
     * 释放锁（确保是自己加的锁才能删除）
     * @param lockKey 锁的 Key
     * @param requestId 请求标识
     * @return 是否成功释放锁
     */
    public boolean releaseLock(String lockKey, String requestId) {
        String value = redisTemplate.opsForValue().get(lockKey);
        if (requestId.equals(value)) {
            return Boolean.TRUE.equals(redisTemplate.delete(lockKey));
        }
        return false;
    }
}
