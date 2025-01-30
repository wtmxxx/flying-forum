package com.atcumt.model.oss.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "file_info")
@TypeAlias("FileInfo")
public class FileInfo {
    @MongoId
    private String filename;
    @Indexed
    private String bucket;
    private Integer version;
    private Long size;
    private List<FileUser> users;
}