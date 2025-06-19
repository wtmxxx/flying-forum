package com.atcumt.ai.service.admin;

import com.atcumt.model.ai.dto.FileDocumentDTO;
import com.atcumt.model.ai.dto.TextDocumentDTO;
import org.springframework.web.multipart.MultipartFile;

public interface AdminAiService {
    void uploadTextDocument(TextDocumentDTO textDocumentDTO);

    void uploadFileDocument(FileDocumentDTO fileDocumentDTO, MultipartFile file);
}
