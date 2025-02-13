package com.atcumt.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import com.atcumt.model.search.entity.SuggestionEs;
import com.atcumt.model.search.vo.SuggestionEsListVO;
import com.atcumt.model.search.vo.SuggestionEsVO;
import com.atcumt.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {
    private final ElasticsearchClient elasticsearchClient;

    @Override
    public SuggestionEsListVO suggest(String text) throws IOException {
        if (text.matches("^[a-z]+('[a-z]+)+$")) {
            text = text.replace("'", "");
        }
        String formatText = text.toLowerCase();

        SearchRequest searchRequest = SearchRequest.of(sr ->
            sr.suggest(s -> s
                    .text(formatText)
                    .suggesters("suggestion", fs -> fs.completion(c -> c
                                    .field("suggestions")
                                    .skipDuplicates(true)
                                    .size(10)
                                    .fuzzy(sf -> sf.fuzziness("AUTO"))
                            ))
                    ).source(SourceConfig.of(sc -> sc.filter(f -> f.includes("suggestionId", "type"))))
        );
        SearchResponse<SuggestionEs> searchResponse = elasticsearchClient.search(searchRequest, SuggestionEs.class);
        List<SuggestionEsVO> suggestionEsVOList = new ArrayList<>();
        for (var option : searchResponse.suggest().get("suggestion").getFirst().completion().options()) {
            SuggestionEs suggestionEs = option.source();
            if (suggestionEs == null) {
                suggestionEs = SuggestionEs.builder().build();
            }

            SuggestionEsVO suggestionEsVO = SuggestionEsVO
                    .builder()
                    .suggestionId(suggestionEs.getSuggestionId())
                    .suggestion(option.text())
                    .type(suggestionEs.getType())
                    .build();
            suggestionEsVOList.add(suggestionEsVO);
        }
        return SuggestionEsListVO.builder().suggestions(suggestionEsVOList).build();
    }
}