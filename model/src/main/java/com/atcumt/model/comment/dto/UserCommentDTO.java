package com.atcumt.model.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserCommentDTO {
    private String userId;
    private String cursor;
    private Long lastCommentId;
    private Integer size;
}
