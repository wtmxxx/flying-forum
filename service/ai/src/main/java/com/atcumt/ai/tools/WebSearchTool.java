package com.atcumt.ai.tools;

import com.atcumt.model.ai.entity.WebSearch;
import com.atcumt.model.ai.entity.WebSearchParameter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RefreshScope
@Component
@Slf4j
@RequiredArgsConstructor
public class WebSearchTool {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<Object, Object> redisTemplate;

    @Value("${flying-forum.searxng.url}")
    private String searxngUrl;
    @Value("${spring-ai.baidu-search.api-key}")
    private String baiduApiKey;
    @Value("${spring-ai.baidu-search.usage-limit}")
    private Integer baiduSearchUsageLimit;
    private final String baiduUrl = "https://qianfan.baidubce.com/v2/ai_search/chat/completions";

    public List<WebSearch> search(WebSearchParameter parameter) {
        List<WebSearch> webSearchList = searxngSearch(parameter);

        if (webSearchList.isEmpty()) {
            webSearchList = baiduSearch(parameter);
        }

        return webSearchList;
    }

    public List<WebSearch> searxngSearch(WebSearchParameter parameter) {
        JsonNode jsonNode = null;
        try {
            jsonNode = webClient.get()
                    .uri(uriBuilder -> {
                        URI baseUri = URI.create(searxngUrl);
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
        } catch (Exception ignored) {}

        if (jsonNode == null) return Collections.emptyList();

        List<WebSearch> webSearchList = new ArrayList<>();
        JsonNode resultsNode = jsonNode.get("results");

        if (resultsNode != null && resultsNode.isArray()) {
            for (JsonNode result : resultsNode) {
                String url = result.path("searxngUrl").asText("");
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

    public List<WebSearch> baiduSearch(WebSearchParameter parameter) {
        if (!checkAndIncrementBaiduSearchUsageLimit()) return Collections.emptyList();

        System.out.println("baiduSearch: " + parameter.getQ());

        ObjectNode messageNode = objectMapper.createObjectNode();
        messageNode.put("role", "user");
        messageNode.put("content", parameter.getQ());

        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("messages", objectMapper.createArrayNode().add(messageNode));

        JsonNode jsonNode = null;
        try {
            jsonNode = webClient.post()
                    .uri(uriBuilder -> {
                        URI baseUri = URI.create(baiduUrl);
                        var builder = uriBuilder
                                .scheme(baseUri.getScheme())
                                .host(baseUri.getHost())
                                .port(baseUri.getPort())
                                .path(baseUri.getPath());

                        return builder.build();
                    })
                    .header("X-Appbuilder-Authorization", "Bearer " + baiduApiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception ignored) {}

        if (jsonNode == null) return Collections.emptyList();

        List<WebSearch> webSearchList = new ArrayList<>();
        JsonNode resultsNode = jsonNode.get("references");

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

    public boolean checkAndIncrementBaiduSearchUsageLimit() {
        String date = LocalDate.now().toString(); // 例如 "2025-05-12"
        String key = "ai:baidu-search:usage-limit:" + date;

        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) count = 1L;

        if (count == 1L) {
            // 设置在第二天凌晨过期
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime tomorrow = now.plusDays(1).toLocalDate().atStartOfDay();
            Duration durationUntilTomorrow = Duration.between(now, tomorrow);
            redisTemplate.expire(key, durationUntilTomorrow);
        }

        return count <= baiduSearchUsageLimit;
    }

    public List<WebSearch> search(WebSearchParameter parameter, int size) {
        List<WebSearch> webSearches = search(parameter);
        return webSearches.subList(0, Math.min(webSearches.size(), size));
    }
}
