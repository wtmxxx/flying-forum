package com.atcumt.post.utils;

import com.atcumt.common.api.forum.sensitive.SensitiveWordDubboService;
import com.atcumt.model.post.entity.Discussion;
import com.atcumt.model.post.entity.Question;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PostReviewUtil {
    @DubboReference
    private SensitiveWordDubboService sensitiveWordDubboService;

    public void review(String title, String content) {
        if (sensitiveWordDubboService.contains(title)) {
            throwTitleException();
        }
        if (sensitiveWordDubboService.contains(content)) {
            throwContentException();
        }
    }

    public void review(Discussion discussion) {
        if (discussion == null) {
            return;
        }

        if (sensitiveWordDubboService.contains(discussion.getTitle())) {
            throwTitleException();
        }
        if (sensitiveWordDubboService.contains(discussion.getContent())) {
            throwContentException();
        }
    }

    public void review(Question question) {
        if (question == null) {
            return;
        }

        if (sensitiveWordDubboService.contains(question.getTitle())) {
            throwTitleException();
        }
        if (sensitiveWordDubboService.contains(question.getContent())) {
            throwContentException();
        }
    }

    private void throwTitleException() {
        throw new IllegalArgumentException("标题含有敏感词，请修改后重试");
    }

    private void throwContentException() {
        throw new IllegalArgumentException("内容含有敏感词，请修改后重试");
    }
}
