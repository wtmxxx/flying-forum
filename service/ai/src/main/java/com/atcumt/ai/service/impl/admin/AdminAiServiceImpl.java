package com.atcumt.ai.service.impl.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.atcumt.ai.service.admin.AdminAiService;
import com.atcumt.ai.utils.VectorUtil;
import com.atcumt.common.enums.PermAction;
import com.atcumt.common.enums.PermModule;
import com.atcumt.common.utils.PermissionUtil;
import com.atcumt.model.ai.dto.FileDocumentDTO;
import com.atcumt.model.ai.dto.TextDocumentDTO;
import com.atcumt.model.ai.entity.KnowledgeBase;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAiServiceImpl implements AdminAiService {
    private final VectorUtil vectorUtil;

    @Override
    public void uploadTextDocument(TextDocumentDTO textDocumentDTO) {
        // 检查权限
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.KNOWLEDGE_BASE, PermAction.CREATE));

        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .title(textDocumentDTO.getTitle())
                .content(textDocumentDTO.getContent())
                .url(textDocumentDTO.getUrl())
                .date(textDocumentDTO.getDate())
                .build();
        vectorUtil.addKnowledgeBase(knowledgeBase);
    }

    @Override
    public void uploadFileDocument(FileDocumentDTO fileDocumentDTO, MultipartFile file) {
        // 检查权限
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.KNOWLEDGE_BASE, PermAction.CREATE));

        // 根据文件类型选择合适的读取器
        List<Document> documents = vectorUtil.readFile(file);

        // 对文档进行处理
        // 使用 TokenTextSplitter 进行文本分割
        TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                // 每个文本块的目标token数量
                .withChunkSize(384)
                // 每个文本块的最小字符数
                .withMinChunkSizeChars(256)
                // 丢弃小于此长度的文本块
                .withMinChunkLengthToEmbed(10)
                // 文本中生成的最大块数
                .withMaxNumChunks(3072)
                // 是否保留分隔符
                .withKeepSeparator(true)
                .build();
        documents = tokenTextSplitter.apply(documents);
        // 使用 KeywordMetadataEnricher 进行元数据增强
        documents = vectorUtil.keywordMetadataEnricher(documents);

        List<KnowledgeBase> knowledgeBases = documents.stream()
                .map(doc -> KnowledgeBase.builder()
                        .title(fileDocumentDTO.getTitle() != null ? fileDocumentDTO.getTitle() : file.getOriginalFilename())
                        .content(doc.getText())
                        .url(fileDocumentDTO.getUrl())
                        .date(fileDocumentDTO.getDate() != null ? fileDocumentDTO.getDate() : LocalDate.now())
                        .metadata(doc.getMetadata())
                        .build())
                .toList();

        vectorUtil.addKnowledgeBases(knowledgeBases);
    }
}
