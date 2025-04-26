package com.atcumt.model.ai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebSearchParameter {
    // http://192.168.200.130:9009/search?q=深度学习&language=auto&time_range=&safesearch=2&categories=general&format=json
    String q;
    @Builder.Default
    String language = "auto";
    String timeRange;
    @Builder.Default
    Integer safeSearch = 2;
    @Builder.Default
    String categories = "general";
}
