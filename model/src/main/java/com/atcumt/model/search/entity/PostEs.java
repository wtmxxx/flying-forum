package com.atcumt.model.search.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "post")
@TypeAlias("post")
public class PostEs {
    @Id
    private Long postId;
    private String postType;
    private String userId;
    private String title;
    private String content;
    @Field(type = FieldType.Keyword)
    private List<String> tags;
    @Field(type = FieldType.Keyword)
    private String status;
    @Field(type = FieldType.Double)
    private Double score;
    @Field(type = FieldType.Date)
    private LocalDateTime createTime;
    @Field(type = FieldType.Date)
    private LocalDateTime updateTime;
}
