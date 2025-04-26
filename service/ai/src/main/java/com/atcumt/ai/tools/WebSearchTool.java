package com.atcumt.ai.tools;

import com.atcumt.model.ai.entity.WebSearch;
import com.atcumt.model.ai.entity.WebSearchParameter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSearchTool {
    private final WebClient webClient;

    @Value("${flying-forum.searxng.url}")
    private String url;

    public List<WebSearch> search(WebSearchParameter parameter) {
        var jsonNode = webClient.get()
                .uri(uriBuilder -> {
                    URI baseUri = URI.create(url);
                    var builder = uriBuilder
                            .scheme(baseUri.getScheme())
                            .host(baseUri.getHost())
                            .port(baseUri.getPort())
                            .path("/search")
                            .queryParam("q", parameter.getQ())
                            .queryParam("language", parameter.getLanguage())
                            .queryParam("safesearch", parameter.getSafeSearch())
                            .queryParam("categories", parameter.getCategories())
                            .queryParam("format", "json");

                    if (parameter.getTimeRange() != null) {
                        builder.queryParam("time_range", parameter.getTimeRange());
                    }

                    return builder.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (jsonNode == null) return Collections.emptyList();

        List<WebSearch> webSearchList = new ArrayList<>();
        JsonNode resultsNode = jsonNode.get("results");

        if (resultsNode != null && resultsNode.isArray()) {
            for (JsonNode result : resultsNode) {
                String url = result.path("url").asText("");
                String title = result.path("title").asText("");
                String content = result.path("content").asText("");

                WebSearch webSearch = new WebSearch();
                webSearch.setUrl(url);
                webSearch.setTitle(title);
                webSearch.setContent(content);

                webSearchList.add(webSearch);
            }
        }

        return webSearchList;
    }

    public List<WebSearch> search(WebSearchParameter parameter, int size) {
        List<WebSearch> webSearches = search(parameter);
        return webSearches.subList(0, Math.min(webSearches.size(), size));
    }
}
