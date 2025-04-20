package com.atcumt.model.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StopConversationMsgDTO {
    private String conversationId;
    private String userId;
    private long timestamp;
}
