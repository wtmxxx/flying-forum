package com.atcumt.model.post.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "tag")
@TypeAlias("Tag")
public class Tag {
    @MongoId
    private Long tagId;
    @Indexed(unique = true)
    private String tagName;
    private Long viewCount;
    private Long usageCount;
    private LocalDateTime createTime;
}
