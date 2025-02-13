package com.atcumt.search.service.impl.admin;

import cn.hutool.core.util.IdUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.CreateOperation;
import com.atcumt.model.search.dto.SuggestionEsDTO;
import com.atcumt.model.search.entity.SuggestionEs;
import com.atcumt.search.service.admin.AdminSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminSearchServiceImpl implements AdminSearchService {
    private final ElasticsearchClient elasticsearchClient;

    @Override
    public void newSuggest(SuggestionEsDTO suggestionEsDTO) throws IOException {
        // TODO: 权限鉴定

        List<BulkOperation> bulkOperations = new ArrayList<>();
        for (String oneDocSuggestion : suggestionEsDTO.getSuggestions()) {
            SuggestionEs suggestionEs = SuggestionEs
                    .builder()
                    .suggestionId(IdUtil.getSnowflakeNextId())
                    .suggestion(oneDocSuggestion)
                    .suggestionText(oneDocSuggestion)
                    .type("custom")
                    .score(0.0)
                    .createTime(LocalDateTime.now())
                    .build();
            CreateOperation<SuggestionEs> createOperation = CreateOperation.of(co -> co
                    .id(String.valueOf(suggestionEs.getSuggestionId()))
                    .index("suggestion")
                    .document(suggestionEs));
            bulkOperations.add(BulkOperation.of(bo -> bo.create(createOperation)));
        }

        BulkRequest bulkRequest = BulkRequest.of(br -> br.operations(bulkOperations));
        elasticsearchClient.bulk(bulkRequest);
    }
}
