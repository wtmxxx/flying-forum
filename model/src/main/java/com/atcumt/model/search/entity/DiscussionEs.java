package com.atcumt.model.search.entity;

import com.atcumt.model.common.entity.MediaFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Document(indexName = "discussion")
@TypeAlias("discussion")
public class DiscussionEs {
    @Field(type = FieldType.Keyword)
    private String userId;
    @Field(type = FieldType.Text)
    private String title;
    @Field(type = FieldType.Text)
    private String content;
    @Field(type = FieldType.Flattened, index = false)
    private List<MediaFile> mediaFiles;
    @Field(type = FieldType.Flattened)
    private List<TagSimpleEs> tags;
    @Field(type = FieldType.Integer)
    private Integer likeCount;
    @Field(type = FieldType.Integer)
    private Integer commentCount;
    @Field(type = FieldType.Keyword)
    private String status;
    @Field(type = FieldType.Double)
    private Double score;
    @Field(type = FieldType.Date)
    private LocalDateTime createTime;
    @Field(type = FieldType.Date)
    private LocalDateTime updateTime;
}
