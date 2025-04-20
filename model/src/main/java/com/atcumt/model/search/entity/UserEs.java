package com.atcumt.model.search.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "user")
@TypeAlias("user")
public class UserEs {
    @Field(type = FieldType.Keyword)
    private String username;
    @Field(type = FieldType.Text)
    private String nickname;
    @Field(type = FieldType.Text)
    private String bio;
    @Field(type = FieldType.Text)
    private String hometown;
    @Field(type = FieldType.Text)
    private String major;
    @Field(type = FieldType.Text)
    private List<String> statuses;
    @Field(type = FieldType.Integer)
    private Integer level;
    @Field(type = FieldType.Integer)
    private Integer experience;
    @Field(type = FieldType.Integer)
    private Integer followersCount;
}
