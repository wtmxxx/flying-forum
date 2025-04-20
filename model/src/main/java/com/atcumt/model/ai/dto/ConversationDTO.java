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
@Schema(description = "对话DTO")
public class ConversationDTO {
    @Schema(name = "conversationId", description = "新对话传null")
    private String conversationId;
    @Schema(name = "parentId", description = "父消息ID")
    private Integer parentId;
    @Schema(name = "content", description = "用户对话内容")
    private String content;
    @Schema(name = "reasoningEnabled", description = "深度思考")
    private Boolean reasoningEnabled = false;
    @Schema(name = "searchEnabled", description = "搜索")
    private Boolean searchEnabled = false;
}
