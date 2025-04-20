package com.atcumt.model.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StreamingMessageVO {
    private String type = "message";
    private Integer messageId;
    private Integer parentId;
    private String model;
    private String role;
    private String content;
}
