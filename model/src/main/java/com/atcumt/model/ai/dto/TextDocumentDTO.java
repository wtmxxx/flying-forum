package com.atcumt.model.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "知识库DTO")
public class TextDocumentDTO {
//    private String id;
    @Schema(description = "标题", example = "中国矿业大学")
    private String title;
    @Schema(description = "内容", example = "中国矿业大学 | CUMT")
    private String content;
    @Schema(description = "文档源URL", example = "https://www.cumt.edu.cn")
    private String url;
    @Schema(description = "文档日期", example = "2005-10-05")
    private LocalDate date;
    @Schema(description = "元数据", example = "{\"author\": \"张三\", \"category\": \"教育\"}")
    private Map<String,Object> metadata;
}
