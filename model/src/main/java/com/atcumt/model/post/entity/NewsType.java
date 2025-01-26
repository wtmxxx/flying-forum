package com.atcumt.model.post.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "news_type")
@TypeAlias("NewsType")
public class NewsType {
    @MongoId
    private Long typeId;
    @Indexed
    private String newsCategory;
    @Indexed
    private String newsType;
    private String shortName;
    @Indexed
    private String sourceName;
}
