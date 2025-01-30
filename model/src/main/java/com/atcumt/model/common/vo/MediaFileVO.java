package com.atcumt.model.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaFileVO {
    private String url;          // 文件地址
    private String fileType;     // 文件类型 (例如: image/jpeg, video/mp4, application/pdf)
}
