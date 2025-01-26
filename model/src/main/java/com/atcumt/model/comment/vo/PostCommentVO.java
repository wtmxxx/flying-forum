package com.atcumt.model.comment.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostCommentVO {
    private String postType;
    private Long postId;
    private Integer size;
    private String sort;
    private String cursor;
    private Long lastCommentId;
    private List<CommentPlusVO> comments;
}
