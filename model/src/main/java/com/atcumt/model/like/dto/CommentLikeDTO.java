package com.atcumt.model.like.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentLikeDTO {
    private String action;
    private String commentType;
    private Long commentId;
    private String userId;
}
