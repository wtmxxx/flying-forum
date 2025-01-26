package com.atcumt.post.utils;

import com.atcumt.model.post.entity.Discussion;
import com.atcumt.model.post.entity.Question;
import com.github.houbb.sensitive.word.core.SensitiveWordHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PostReviewUtil {
    public static void review(String title, String content) {
        if (SensitiveWordHelper.contains(title)) {
            throwTitleException();
        }
        if (SensitiveWordHelper.contains(content)) {
            throwContentException();
        }
    }

    public static void review(Discussion discussion) {
        if (discussion == null) {
            return;
        }

        if (SensitiveWordHelper.contains(discussion.getTitle())) {
            throwTitleException();
        }
        if (SensitiveWordHelper.contains(discussion.getContent())) {
            throwContentException();
        }
    }

    public static void review(Question question) {
        if (question == null) {
            return;
        }

        if (SensitiveWordHelper.contains(question.getTitle())) {
            throwTitleException();
        }
        if (SensitiveWordHelper.contains(question.getContent())) {
            throwContentException();
        }
    }

    private static void throwTitleException() {
        throw new IllegalArgumentException("标题含有敏感词，请修改后重试");
    }

    private static void throwContentException() {
        throw new IllegalArgumentException("内容含有敏感词，请修改后重试");
    }
}
