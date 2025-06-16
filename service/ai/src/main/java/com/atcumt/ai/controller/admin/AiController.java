package com.atcumt.ai.controller.admin;

import com.atcumt.ai.service.admin.AdminAiService;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.ai.dto.KnowledgeBaseDTO;
import com.atcumt.model.common.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("adminAiControllerV1")
@RequestMapping("/api/ai/admin/v1")
@Tag(name = "AdminAI", description = "管理员AI相关接口")
@RequiredArgsConstructor
@Slf4j
public class AiController {
    private final AdminAiService adminAiService;

    @PostMapping(path = "/knowledge_base/text")
    @Operation(summary = "上传纯文本文档")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> uploadTextDocument(@RequestBody KnowledgeBaseDTO knowledgeBaseDTO) {
        log.info("上传纯文本文档, userId: {}, title: {}", UserContext.getUserId(), knowledgeBaseDTO.getTitle());

        adminAiService.uploadTextDocument(knowledgeBaseDTO);

        return Result.success();
    }
}
