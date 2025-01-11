package com.atcumt.model.like.vo;

import cn.hutool.json.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostLikeVO {
    private Long likeId;
    private String action;
    private String postType;
    private Long postId;
    private JSONObject postInfo; // 帖子信息，不同帖子依据VO
    private String userId;
    private LocalDateTime createTime;
}
