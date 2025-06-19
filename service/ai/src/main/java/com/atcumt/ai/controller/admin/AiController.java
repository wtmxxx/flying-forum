package com.atcumt.ai.controller.admin;

import com.atcumt.ai.service.admin.AdminAiService;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.ai.dto.FileDocumentDTO;
import com.atcumt.model.ai.dto.TextDocumentDTO;
import com.atcumt.model.common.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController("adminAiControllerV1")
@RequestMapping("/api/ai/admin/v1")
@Tag(name = "AdminAI", description = "管理员AI相关接口")
@RequiredArgsConstructor
@Slf4j
public class AiController {
    private final AdminAiService adminAiService;

    @PostMapping(path = "/knowledge_base/text")
    @Operation(summary = "上传纯文本知识库")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> uploadTextDocument(@RequestBody TextDocumentDTO textDocumentDTO) {
        log.info("上传纯文本文档, userId: {}, title: {}", UserContext.getUserId(), textDocumentDTO.getTitle());

        adminAiService.uploadTextDocument(textDocumentDTO);

        return Result.success();
    }

    @PostMapping(path = "/knowledge_base/file")
    @Operation(summary = "上传文件知识库")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "file", description = "文件", required = true)
    })
    public Result<Object> uploadFileDocument(@RequestPart(value = "params") FileDocumentDTO fileDocumentDTO,
                                             @RequestPart(value = "file") MultipartFile file
    ) {
        log.info("上传文件知识库, userId: {}, title: {}", UserContext.getUserId(), fileDocumentDTO.getTitle());

        adminAiService.uploadFileDocument(fileDocumentDTO, file);

        return Result.success();
    }

}
