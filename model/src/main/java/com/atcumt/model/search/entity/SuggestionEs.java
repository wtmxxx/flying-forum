package com.atcumt.model.search.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "suggestion")
@TypeAlias("suggestion")
public class SuggestionEs {
    @Field(type = FieldType.Text)
    private String suggestion;
    @Field(type = FieldType.Double)
    private Double score;
    @Field(type = FieldType.Keyword)
    private String type;
    @Field(type = FieldType.Date)
    private LocalDateTime createTime;
}