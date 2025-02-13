package com.atcumt.model.forum.sensitive.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SensitiveWordVO {
    private Long wordId;
    private String word;
    private String type;
    private String tag;
    private String createTime;
    private String updateTime;
}
