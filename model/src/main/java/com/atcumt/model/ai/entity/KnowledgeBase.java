package com.atcumt.model.ai.entity;

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
public class KnowledgeBase {
    private String id;
    private String title;
    private String content;
    private String url;
    private LocalDate date;
    private Map<String, Object> metadata; // 可选的元数据字段，用于存储额外信息
}
