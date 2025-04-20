package com.atcumt.model.search.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchVO {
    private Integer from;
    private Integer size;
    private List<SearchEsVO> hits;
}
