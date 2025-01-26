package com.atcumt.model.like.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostUserLikeDTO {
    private String postType;
    private Long postId;
    private String cursor;
    private Long lastLikeId;
    private Integer size;
}
