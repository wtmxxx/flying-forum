package com.atcumt.ai.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Component
public class SseEmitterRegistry {

    // conversationId -> SseEmitter
    private static final int MAX_SIZE = 10_000; // 最大连接数
    private static final int EXPIRE_MINUTES = 10; // 过期时间（分钟）
    private static final Cache<String, SseEmitter> emitters = Caffeine.newBuilder()
            .expireAfterWrite(EXPIRE_MINUTES, TimeUnit.MINUTES)
            .maximumSize(MAX_SIZE)
            .build();

    public SseEmitter create(String userId, String conversationId) {
        if (emitters.estimatedSize() >= MAX_SIZE) {
            throw new RuntimeException("连接数已达上限，请稍后重试");
        }

        SseEmitter emitter = new SseEmitter(600_000L);
        emitter.onTimeout(() -> {
            emitter.complete();
            remove(userId, conversationId);
        });
        emitter.onError((e) -> {
            emitter.complete();
            remove(userId, conversationId);
        });
        emitter.onCompletion(() -> {
            remove(userId, conversationId);
        });
        create(userId, conversationId, emitter);
        return emitter;
    }

    public void create(String userId, String conversationId, SseEmitter emitter) {
        emitters.put(getKey(userId, conversationId), emitter);
    }

    public SseEmitter get(String userId, String conversationId) {
        return emitters.getIfPresent(getKey(userId, conversationId));
    }

    public void remove(String userId, String conversationId) {
        SseEmitter sseEmitter = get(userId, conversationId);
        if (sseEmitter != null) {
            try { sseEmitter.complete(); } catch (Exception ignored) {}
            emitters.invalidate(getKey(userId, conversationId));
        }
    }

    public boolean contains(String userId, String conversationId) {
        return emitters.getIfPresent(getKey(userId, conversationId)) != null;
    }

    public Collection<SseEmitter> getAll() {
        return emitters.asMap().values();
    }

    private String getKey(String userId, String conversationId) {
        return userId + "_" + conversationId;
    }
}
