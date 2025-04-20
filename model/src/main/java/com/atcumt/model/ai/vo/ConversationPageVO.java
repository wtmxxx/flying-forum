package com.atcumt.model.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationPageVO {
    private String conversationId;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
