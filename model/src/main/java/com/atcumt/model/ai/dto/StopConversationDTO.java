package com.atcumt.model.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "中止对话DTO")
public class StopConversationDTO {
    @Schema(name = "conversationId", description = "对话ID")
    private String conversationId;
}
