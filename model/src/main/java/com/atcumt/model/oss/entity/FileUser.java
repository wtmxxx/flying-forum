package com.atcumt.model.oss.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileUser {
    private String userId;
    private String originalFilename;
    private LocalDateTime uploadTime;
}
