package com.atcumt.model.forum.sensitive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SensitiveWordDTO {
    private String word;    // 敏感词
    private String type;    // 类型
    private String tag;     // 标签
}
