package com.atcumt.model.ai.vo;

import com.atcumt.model.ai.entity.WebSearch;
import com.atcumt.model.ai.enums.FluxType;
import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class WebSearchResultsVO extends FluxVO {
    @Builder.Default
    private String type = FluxType.WEB_SEARCH_MESSAGE.getValue();
    private List<WebSearch> searchResults;
}
