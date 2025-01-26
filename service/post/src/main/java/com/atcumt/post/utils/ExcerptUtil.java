package com.atcumt.post.utils;

public class ExcerptUtil {
    public static String getExcerpt(String text, int minLength, int maxLength) {
        if (text == null || text.isEmpty()) return null;

        // 去除换行符
        text = text.replaceAll("\n", "").trim();

        if (text.length() <= minLength) {
            return text;
        }

        // 按句号、问号、感叹号分句
        String[] sentences = text.splitWithDelimiters("[，。,. ]", maxLength);
        StringBuilder excerpt = new StringBuilder();

        for (String sentence : sentences) {
            if (excerpt.length() + sentence.length() > maxLength) {
                break;
            }
            excerpt.append(sentence);
            // 如果当前长度大于等于最小长度，且不超出最大长度，则返回结果
            if (excerpt.length() >= minLength) {
                return excerpt.toString();
            }
        }

        if (excerpt.length() < minLength) {
            // 如果无法达到最小长度，直接返回当前已拼接的内容
            return excerpt.substring(0, Math.min(excerpt.length(), maxLength));
        }

        // 如果无法达到最小长度，直接返回当前已拼接的内容
        return excerpt.toString();
    }

    public static String getExcerpt(String text) {
        return getExcerpt(text, 50, 100);
    }
}
