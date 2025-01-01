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
public class CommentReplyVO {
    private Long commentId;
    private String cursor;
    private Long lastReplyId;
    private Integer size;
    private List<ReplyPlusVO> replies;
}
