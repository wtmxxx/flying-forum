package com.atcumt.model.comment.dto;

import com.atcumt.model.common.dto.MediaFileDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReplyDTO {
    @Schema(description = "回复 ID")
    private Long replyToId;
    @Schema(description = "是否为根评论回复")
    private Boolean isRoot;
    private String content;
    private List<MediaFileDTO> mediaFiles;
}
