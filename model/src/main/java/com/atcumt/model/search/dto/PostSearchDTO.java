package com.atcumt.model.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostSearchDTO {
    private String text;
    private String searchContentType;
    private String searchSortType;
    private String searchTimeLimit;
    private Integer from;
    private Integer size;
}
