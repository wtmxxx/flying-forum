package com.atcumt.model.like.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserPostLikeVO {
    private Integer size;
    private String cursor;
    private Long lastLikeId;
    private List<PostLikeVO> likes;
}
