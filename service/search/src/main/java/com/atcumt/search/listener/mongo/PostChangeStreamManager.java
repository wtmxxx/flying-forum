package com.atcumt.search.listener.mongo;

import com.atcumt.model.post.enums.PostType;
import com.atcumt.search.listener.mongo.template.PostChangeStreamsListenerFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostChangeStreamManager {
    private final PostChangeStreamsListenerFactory factory;

    @PostConstruct
    public void init() {
        log.info("🔍 PostChangeStreamManager 初始化完成，开始监听MongoDB Change Streams");

        // 监听所有帖子类型的MongoDB Change Streams
        for (PostType postType : PostType.values()) {
            var listener = factory.createListener(postType.getValue());
            listener.startListening();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                log.warn("🔍 PostChangeStreamManager 线程被中断: {}", e.getMessage());
            }
        }
    }
}