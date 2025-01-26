package com.atcumt.model.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewsListDTO {
    private String newsCategory;
    private String newsType;
    private String sourceName;
    private String cursor;
    private Long lastNewsId;
    private Integer size;
    private String sort;
}
