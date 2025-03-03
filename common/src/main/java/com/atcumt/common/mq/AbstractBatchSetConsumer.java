package com.atcumt.common.mq;

import cn.hutool.json.JSONObject;
import com.atcumt.common.utils.ExecutorUtil;
import com.atcumt.model.common.annotation.BatchSetConsumerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.mongodb.core.BulkOperations;

import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class AbstractBatchSetConsumer<T> implements RocketMQListener<JSONObject> {
    private int BATCH_SIZE = 20;
    private int CONSUME_SIZE = BATCH_SIZE + (BATCH_SIZE >> 1);
    private String MESSAGE_LOG = "BatchSetConsumer";
    private final Set<T> messageQueue = ConcurrentHashMap.newKeySet(BATCH_SIZE + 1);
    private final AtomicBoolean isConsuming = new AtomicBoolean(false);

    {
        // 解析子类的 @BatchSetConsumerConfig 注解
        BatchSetConsumerConfig config = this.getClass().getAnnotation(BatchSetConsumerConfig.class);
        if (config != null) {
            BATCH_SIZE = config.batchSize();
            if (BATCH_SIZE <= 0) {
                BATCH_SIZE = 20;
            }
            CONSUME_SIZE = config.consumeSize();
            if (CONSUME_SIZE <= 0) {
                CONSUME_SIZE = BATCH_SIZE + (BATCH_SIZE >> 1);
            }
            MESSAGE_LOG = config.messageLog();
        }
    }

    public Class<T> getEntityClass() {
        try {
            return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onMessage(JSONObject message) {
        messageQueue.add(message.toBean(getEntityClass()));
        if (messageQueue.size() >= BATCH_SIZE) {
            log.info("{}: 消息堆积达到阈值，触发批量消费...", MESSAGE_LOG);
            batchConsume();
        }
    }

    public void scheduledCount() {
        if (!messageQueue.isEmpty()) {
            log.info("{}: 定时任务触发批量消费...", MESSAGE_LOG);
            batchConsume();
        }
    }

    public void batchConsume() {
        if (!isConsuming.compareAndSet(false, true)) {
            // 其他线程正在处理，直接返回
            return;
        }

        List<T> messages = new ArrayList<>();
        try {
            Iterator<T> iterator = messageQueue.iterator();
            while (iterator.hasNext() && messages.size() < CONSUME_SIZE) {
                messages.add(iterator.next());
            }
            messages.forEach(messageQueue::remove);
        } finally {
            isConsuming.set(false);
        }

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Map<String, BulkOperations> bulkOpsMap = new ConcurrentHashMap<>();

        for (var message : messages) {
            executor.submit(() -> consumeMessage(message, bulkOpsMap));
        }
        // 等待所有任务完成
        ExecutorUtil.shutdown(executor);
        executeBulkOperations(bulkOpsMap);
    }

    public abstract void consumeMessage(T message, Map<String, BulkOperations> bulkOpsMap);

    private void executeBulkOperations(Map<String, BulkOperations> bulkOpsMap) {
        for (BulkOperations bulkOps : bulkOpsMap.values()) {
            try {
                bulkOps.execute();
            } catch (Exception e) {
                log.error("{}: 批量更新时发生错误", MESSAGE_LOG, e);
            }
        }
    }
}