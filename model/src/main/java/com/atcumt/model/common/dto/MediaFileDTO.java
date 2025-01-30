package com.atcumt.model.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaFileDTO {
    private String bucket;
    private String fileName;
    private String fileType;
}