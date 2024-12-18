package com.atcumt.model.forum.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaFileVO {
    private String url;          // 文件地址
    private String bucket;
    private String fileName;     // 文件名
    private String customName;    // 自定义文件名（可选）
    private String description;   // 文件描述（可选）
    private String fileType;     // 文件类型 (例如: image/jpeg, video/mp4, application/pdf)
    private Long fileSize;        // 文件大小，单位：字节
    private LocalDateTime uploadTime;  // 上传时间
}
