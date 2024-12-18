package com.atcumt.model.oss.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileInfoVO {
    private String url;
    private String bucket;
    private String fileName;
    private String originalName;    // 原始文件名
    private String contentType;
    private Long size;
    private LocalDateTime uploadTime;
}
