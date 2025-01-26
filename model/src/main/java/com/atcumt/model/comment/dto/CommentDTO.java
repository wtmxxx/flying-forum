package com.atcumt.model.comment.dto;

import com.atcumt.model.common.dto.MediaFileDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentDTO {
    private String postType;
    private Long postId;
    private String content;
    private List<MediaFileDTO> mediaFiles;
}
