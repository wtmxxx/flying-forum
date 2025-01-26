package com.atcumt.model.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OnePostFeedDTO {
    private String postType;
    private String cursor;
    private Long lastPostId;
    private Integer size;
}
