package com.atcumt.model.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationVO {
    private String conversationId;
    private String userId;
    private String title;
    private Integer currentMessageId;
    private List<MessageVO> messages;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
