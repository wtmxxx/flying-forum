package com.atcumt.model.search.dto;

import com.atcumt.model.search.enums.SuggestionAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchSuggestionDTO {
    private SuggestionAction action;
    private String suggestion;
    private String type;
}