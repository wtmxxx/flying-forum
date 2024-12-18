package com.atcumt.model.oss.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "file")
public class FileInfo {
    @MongoId
    private Long fileId;
    private String bucket;
    private String fileName;
    private String contentType;
    private Long size;
    private LocalDateTime uploadTime;
}