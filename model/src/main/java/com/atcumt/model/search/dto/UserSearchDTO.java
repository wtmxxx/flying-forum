package com.atcumt.model.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSearchDTO {
    private String text;
    private String searchSortType;
    private Integer from;
    private Integer size;
}
