package com.atcumt.model.ai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "conversation")
@TypeAlias("Conversation")
public class Conversation {
    @MongoId
    private String conversationId;
    @Indexed
    private String userId;
    private String title;
    private Integer currentMessageId;
    private List<Message> messages;
    @Indexed(direction = IndexDirection.DESCENDING)
    private LocalDateTime createTime;
    @Indexed(direction = IndexDirection.DESCENDING)
    private LocalDateTime updateTime;
}