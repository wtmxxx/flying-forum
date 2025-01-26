package com.atcumt.model.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostCommentDTO {
    private String postType;
    private Long postId;
    private String cursor;
    private Long lastCommentId;
    private Integer size;
    private String sort;
}
