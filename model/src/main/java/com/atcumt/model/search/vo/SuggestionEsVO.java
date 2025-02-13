package com.atcumt.model.search.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SuggestionEsVO {
    private Long suggestionId;
    private String suggestion;
    private String type;
}