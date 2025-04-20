package com.atcumt.model.search.vo;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class TagEsVO extends SearchEsVO {
    private Long tagId;
    private String tagName;
    private Long viewCount;
    private Long usageCount;
    private LocalDateTime createTime;
}
