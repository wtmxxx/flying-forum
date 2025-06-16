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
@Schema(description = "知识库DTO")
public class KnowledgeBaseDTO {
//    private String id;
    @Schema(description = "标题", example = "中国矿业大学")
    private String title;
    @Schema(description = "内容", example = "中国矿业大学 | CUMT")
    private String content;
    @Schema(description = "文档源URL", example = "https://www.cumt.edu.cn")
    private String url;
}
