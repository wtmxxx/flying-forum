package com.atcumt.ai.utils;

import com.atcumt.model.ai.entity.KnowledgeBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.id.JdkSha256HexIdGenerator;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class VectorUtil {
    private final VectorStore vectorStore;

    public void addTextDocument(KnowledgeBase knowledgeBase) {
        // 构造 Document
        Map<String, Object> metadata = Map.of(
                "title", knowledgeBase.getTitle(),
                "url", knowledgeBase.getUrl()
        );

        Document document = Document.builder()
                .id(new JdkSha256HexIdGenerator().generateId(knowledgeBase.getContent()))
                .text(knowledgeBase.getContent())
                .metadata(metadata)
                .build();

        vectorStore.add(List.of(document));
    }

    public List<Document> similaritySearch(String query, int topK) {
        log.info("相似搜索, query: {}, topK: {}", query, topK);
        return vectorStore.similaritySearch(SearchRequest
                .builder()
                .query(query)
                .similarityThreshold(0.5)
                .topK(topK)
                .build());
    }

}
