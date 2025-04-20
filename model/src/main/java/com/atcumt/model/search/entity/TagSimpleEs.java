package com.atcumt.model.search.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TagSimpleEs {
    @Field(type = FieldType.Long)
    private Long tagId;
    @Field(type = FieldType.Text)
    private String tagName;
}
