package com.atcumt.ai.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.TimeUnit;

@Component
public class ConversationManager {

    private static final int MAX_SIZE = 10_000;
    private static final int EXPIRE_MINUTES = 10;

    // userId_conversationId -> cancel signal sink
    private static final Cache<String, Sinks.Many<Object>> cancelSignals = Caffeine.newBuilder()
            .expireAfterWrite(EXPIRE_MINUTES, TimeUnit.MINUTES)
            .maximumSize(MAX_SIZE)
            .build();

    public void register(String userId, String conversationId) {
        cancelSignals.put(getKey(userId, conversationId), Sinks.many().multicast().onBackpressureBuffer());
    }

    public boolean contains(String userId, String conversationId) {
        return cancelSignals.getIfPresent(getKey(userId, conversationId)) != null;
    }

    public void cancel(String userId, String conversationId) {
        Sinks.Many<Object> sink = cancelSignals.getIfPresent(getKey(userId, conversationId));
        if (sink != null) {
            sink.tryEmitNext(new Object());
        }
        remove(userId, conversationId);
    }

    public Flux<Object> cancelFlux(String userId, String conversationId) {
        Sinks.Many<Object> sink = cancelSignals.getIfPresent(getKey(userId, conversationId));
        return sink != null ? sink.asFlux() : Flux.never();
    }

    public void remove(String userId, String conversationId) {
        cancelSignals.invalidate(getKey(userId, conversationId));
    }

    private String getKey(String userId, String conversationId) {
        return userId + "_" + conversationId;
    }
}
