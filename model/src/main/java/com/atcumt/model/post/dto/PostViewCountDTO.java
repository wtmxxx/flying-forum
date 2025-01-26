package com.atcumt.model.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostViewCountDTO {
    private String postType;
    private Long postId;
    private List<Long> tagIds;
}
