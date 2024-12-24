package com.atcumt.model.post.dto;

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
public class DiscussionDTO {
    private String title;  // 帖子标题
    private String content;  // 帖子内容
    private List<MediaFileDTO> mediaFiles;  // 动态存储附件数据：图片、视频、文档等，每个媒体文件包含文件类型、URL等信息
    private List<Long> tagIds;  // 标签ID列表
}
