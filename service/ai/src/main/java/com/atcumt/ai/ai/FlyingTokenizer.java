package com.atcumt.ai.ai;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

public class FlyingTokenizer {
    private final int maxTokens;

    public FlyingTokenizer() {
        this.maxTokens = 20000;
    }

    public FlyingTokenizer(int maxTokens) {
        this.maxTokens = maxTokens * 4;
    }

    public int estimateTokenCountInText(String text) {
        if (text == null) return 0;
        if (text.length() <= maxTokens || text.length() > maxTokens * 4) return text.length() * 3 / 4;

        int chineseChars = 0;
        int otherChars = 0;

        for (char c : text.toCharArray()) {
            if (isChinese(c)) {
                chineseChars++;
            } else if (!Character.isWhitespace(c)) {  // 忽略空格
                otherChars++;
            }
        }

        // 估算规则：
        // 中文/日文/韩文：1个Token ≈ 2个字符
        // 其他字符（含英文）：1个Token ≈ 4个字符
        return (int) Math.ceil(chineseChars / 2.0 + otherChars / 4.0);
    }

    // 判断是否为中日韩字符（包括标点）
    private boolean isChinese(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || block == Character.UnicodeBlock.GENERAL_PUNCTUATION;
    }

//    public int estimateTokenCountInMessages(List<Message> messages) {
//        int count = 0;
//        for (Message msg : messages) {
//            count += estimateTokenCountInMessage(msg);
//        }
//        return count;
//    }

    public int estimateTokenCountInMessages(Iterable<Message> messages) {
        int count = 0;
        for (Message msg : messages) {
            count += estimateTokenCountInMessage(msg);
        }
        return count;
    }

    public int estimateTokenCountInMessage(Message message) {
        if (message instanceof UserMessage userMessage) {
            return estimateTokenCountInText(userMessage.getText());
        } else if (message instanceof AssistantMessage assistantMessage) {
            return estimateTokenCountInText(assistantMessage.getText());
        }

        return estimateTokenCountInText(message.getMessageType().getValue());
    }
}
