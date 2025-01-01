package com.atcumt.model.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentReplyDTO {
    private Long commentId;
    private String cursor;
    private Long lastReplyId;
    private Integer size;
}
