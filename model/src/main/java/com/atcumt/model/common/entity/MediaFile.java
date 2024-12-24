package com.atcumt.model.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaFile {
    private String url;           // 文件链接
    private String bucket;        // 文件桶
    private String fileName;      // 文件名
    private String customName;    // 自定义文件名
    private String description;   // 文件描述（可选）
    private String fileType;      // 文件类型 (例如: image/jpeg, video/mp4, application/pdf)
}
