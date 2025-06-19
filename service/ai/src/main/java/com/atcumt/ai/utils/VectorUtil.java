package com.atcumt.ai.utils;

import cn.hutool.core.io.file.FileNameUtil;
import com.atcumt.model.ai.entity.KnowledgeBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.id.JdkSha256HexIdGenerator;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.jsoup.config.JsoupDocumentReaderConfig;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class VectorUtil {
    private final VectorStore vectorStore;
    private final ChatModel dashScopeChatModel;

    public Document toDocument(KnowledgeBase knowledgeBase) {
        // 构造 Document
        Map<String, Object> metadata = new HashMap<>(Map.of(
                "title", knowledgeBase.getTitle(),
                "url", knowledgeBase.getUrl(),
                "date", knowledgeBase.getDate()
        ));
        // 如果有其他元数据字段，可以在这里添加
        if (knowledgeBase.getMetadata() != null) {
            metadata.putAll(knowledgeBase.getMetadata());
        }

        return Document.builder()
                .id(new JdkSha256HexIdGenerator().generateId(knowledgeBase.getContent()))
                .text(knowledgeBase.getContent())
                .metadata(metadata)
                .build();
    }

    public void addKnowledgeBase(KnowledgeBase knowledgeBase) {
        vectorStore.add(List.of(toDocument(knowledgeBase)));
    }

    public void addKnowledgeBases(List<KnowledgeBase> knowledgeBases) {
        vectorStore.add(knowledgeBases.stream().map(this::toDocument).toList());
    }

    public List<Document> readFile(MultipartFile file) {
        Resource resource = file.getResource();

        return switch (FileNameUtil.extName(file.getOriginalFilename())) {
            case null -> List.of();
            case "txt" -> new TextReader(resource).read();
            case "json" -> new JsonReader(resource).read();
            case "pdf" -> new ParagraphPdfDocumentReader(resource, PdfDocumentReaderConfig
                    .builder()
                    .withPagesPerDocument(1)                     // 每页作为一个独立文档（适合用于向量数据库存储或逐页分析）
                    .withReversedParagraphPosition(false)        // 段落顺序是否颠倒（一般 false，除非文档格式异常）
                    .withPageExtractedTextFormatter(
                            ExtractedTextFormatter.builder()
                                    .withLeftAlignment(true)
                                    .overrideLineSeparator("\n")
                                    .build()
                    )
                    .build()
            ).read();
            case "md", "markdown" -> new MarkdownDocumentReader(resource, MarkdownDocumentReaderConfig
                    .builder()
                    .withHorizontalRuleCreateDocument(true)      // 是否将水平线转换为独立文档
                    .withIncludeBlockquote(true)                 // 是否包含引用块
                    .withIncludeCodeBlock(true)                  // 是否包含代码块
                    .build()
            ).read();
            case "html", "htm" -> new JsoupDocumentReader(resource, JsoupDocumentReaderConfig
                    .builder()
                    .charset("UTF-8")                             // 设置字符集
                    .selector("p")                                // 设置要提取的元素选择器，如提取 <article> 标签内容
                    .separator("\n")                              // 多个元素间的分隔符
                    .allElements(true)                            // 是否抓取所有匹配的元素
                    .groupByElement(true)                         // 是否将每个元素作为独立文档处理
                    .includeLinkUrls(true)                        // 是否将链接的 URL 包含在内容中
                    .build()
            ).read();
            default -> new TikaDocumentReader(resource, ExtractedTextFormatter
                    .builder()
                    .overrideLineSeparator("\n")                   // 设置覆盖的行分隔符
                    .withLeftAlignment(true)                       // 设置文本左对齐
                    .build()
            ).read();
        };
    }

    public List<Document> keywordMetadataEnricher(List<Document> documents) {
        KeywordMetadataEnricher keywordMetadataEnricher = new KeywordMetadataEnricher(dashScopeChatModel, 3);
        return keywordMetadataEnricher.apply(documents);
    }
}
