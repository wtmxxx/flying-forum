package com.atcumt.model.post.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TagVO {
    private Long tagId;
    private String tagName;
    private Long viewCount;
    private Long usageCount;
    private LocalDateTime createTime;
}
