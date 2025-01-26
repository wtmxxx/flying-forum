package com.atcumt.model.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostFeedDTO {
    @Schema(description = "帖子类型")
    private String postType;
    @Schema(description = "游标")
    private String cursor;
    @Schema(description = "最后一条帖子ID, postType -> lastPostId")
    private Map<String, Long> lastPostIds;
    @Schema(description = "数量")
    private Integer size;
}
