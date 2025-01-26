package com.atcumt.model.post.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostFeedListVO {
    private Integer size;
    private String cursor;
    private Map<String, Long> lastPostIds;
    private List<PostFeedVO> posts;
}
