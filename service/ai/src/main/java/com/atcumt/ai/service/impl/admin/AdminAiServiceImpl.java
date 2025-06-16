package com.atcumt.ai.service.impl.admin;

import com.atcumt.ai.service.admin.AdminAiService;
import com.atcumt.ai.utils.VectorUtil;
import com.atcumt.model.ai.dto.KnowledgeBaseDTO;
import com.atcumt.model.ai.entity.KnowledgeBase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAiServiceImpl implements AdminAiService {
    private final VectorUtil vectorUtil;

    @Override
    public void uploadTextDocument(KnowledgeBaseDTO knowledgeBaseDTO) {
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .title(knowledgeBaseDTO.getTitle())
                .content(knowledgeBaseDTO.getContent())
                .url(knowledgeBaseDTO.getUrl())
                .build();
        vectorUtil.addTextDocument(knowledgeBase);
    }
}
