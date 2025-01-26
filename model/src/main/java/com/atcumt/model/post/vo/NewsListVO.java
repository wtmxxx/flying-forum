package com.atcumt.model.post.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewsListVO {
    private String newsCategory;
    private String newsType;
    private String sourceName;
    private Integer size;
    private String cursor;
    private Long lastNewsId;
    private List<NewsSimpleVO> newsList;
}
