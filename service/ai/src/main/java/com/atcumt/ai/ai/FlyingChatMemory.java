package com.atcumt.ai.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;

import java.util.LinkedList;
import java.util.List;

@Slf4j
public class FlyingChatMemory implements ChatMemory {
    private final Integer maxTokens;
    private final Integer maxMessages;
    private final TokenCountEstimator tokenizer;
//    private final FlyingChatMemoryRepository repository;
    private final List<Message> messages = new LinkedList<>();

    public FlyingChatMemory() {
        this.maxTokens = 20000;
        this.maxMessages = 32;
        this.tokenizer = new JTokkitTokenCountEstimator();
//        this.repository = new FlyingChatMemoryRepository();
    }

    public FlyingChatMemory(int maxTokens, int maxMessages, TokenCountEstimator tokenizer) {
        this.maxTokens = maxTokens;
        this.maxMessages = maxMessages;
        this.tokenizer = tokenizer;
//        this.repository = new FlyingChatMemoryRepository();
    }

    public void add(Message message) {
        this.messages.add(message);
        ensureCapacity(this.messages, this.maxTokens, this.maxMessages);
    }

    public void add(List<Message> messages) {
        this.messages.addAll(messages);
        ensureCapacity(this.messages, this.maxTokens, this.maxMessages);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        this.messages.addAll(messages);
        ensureCapacity(this.messages, this.maxTokens, this.maxMessages);
    }

    @Override
    public List<Message> get(String conversationId) {
        return this.messages;
    }

    @Override
    public void clear(String conversationId) {
        messages.clear();
    }

    public void clear() {
        messages.clear();
    }

    private void ensureCapacity(List<Message> messages, int maxTokens, int maxMessages) {
        if (!messages.isEmpty()) {
            int currentTokenCount = this.estimateTokenCountInMessages(messages);

            while(currentTokenCount > maxTokens && !messages.isEmpty()) {
                int messageToEvictIndex = 0;
                if (messages.getFirst() instanceof SystemMessage) {
                    if (messages.size() == 1) {
                        return;
                    }

                    messageToEvictIndex = 1;
                }

                Message evictedMessage = messages.remove(messageToEvictIndex);
                int tokenCountOfEvictedMessage = this.estimateTokenCountInMessage(evictedMessage);
                log.trace("Evicting the following message ({} tokens) to comply with the capacity requirement: {}", tokenCountOfEvictedMessage, evictedMessage);
                currentTokenCount -= tokenCountOfEvictedMessage;
                if (evictedMessage instanceof AssistantMessage && ((AssistantMessage)evictedMessage).hasToolCalls()) {
                    while(messages.size() > messageToEvictIndex && messages.get(messageToEvictIndex) instanceof ToolResponseMessage) {
                        Message orphanToolExecutionResultMessage = messages.remove(messageToEvictIndex);
                        log.trace("Evicting orphan {}", orphanToolExecutionResultMessage);
                        currentTokenCount -= this.estimateTokenCountInMessage(orphanToolExecutionResultMessage);
                    }
                }
            }

            if (messages.size() > maxMessages) {
                int messagesToEvict = messages.size() - maxMessages;
                for (int i = 0; i < messagesToEvict; i++) {
                    Message evictedMessage = messages.removeLast();
                    log.trace("Evicting the following message to comply with the capacity requirement: {}", evictedMessage);
                }
            }
        }
    }

    public int estimateTokenCountInMessages(Iterable<Message> messages) {
        int count = 0;
        for (Message msg : messages) {
            count += estimateTokenCountInMessage(msg);
        }
        return count;
    }

    public int estimateTokenCountInMessage(Message message) {
        if (message instanceof UserMessage userMessage) {
            return this.tokenizer.estimate(userMessage.getText());
        } else if (message instanceof AssistantMessage assistantMessage) {
            return this.tokenizer.estimate(assistantMessage.getText());
        }

        return this.tokenizer.estimate(message.getMessageType().getValue());
    }
}
