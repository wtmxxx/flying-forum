package com.atcumt.post.controller.admin.v1;

import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.post.dto.QuestionUpdateDTO;
import com.atcumt.model.post.vo.QuestionPostVO;
import com.atcumt.post.service.QuestionService;
import com.atcumt.post.service.admin.AdminQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("questionControllerAdminV1")
@RequestMapping("/api/post/admin/question/v1")
@Tag(name = "AdminQuestion", description = "管理员问答相关接口")
@RequiredArgsConstructor
@Slf4j
public class QuestionController {
    private final QuestionService questionService;
    private final AdminQuestionService adminQuestionService;

    @PatchMapping("")
    @Operation(summary = "修改问答", description = "修改问答")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<QuestionPostVO> updateQuestion(@RequestBody QuestionUpdateDTO questionUpdateDTO) throws AuthorizationException {
        log.info("修改问答, authorId: {}", UserContext.getUserId());

        QuestionPostVO questionPostVO = adminQuestionService.updateQuestion(questionUpdateDTO);

        return Result.success(questionPostVO);
    }

    @DeleteMapping("")
    @Operation(summary = "删除问答", description = "删除问答")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "questionId", description = "问答ID", required = true),
            @Parameter(name = "isLogic", description = "是否为逻辑删除，默认为true")
    })
    public Result<Object> deleteQuestion(Long questionId, Boolean isLogic) {
        log.info("删除问答, authorId: {}", UserContext.getUserId());

        if (isLogic == null) isLogic = true;

        // 删除帖子
        if (isLogic) adminQuestionService.deleteQuestion(questionId);
        else adminQuestionService.deleteQuestionComplete(questionId);

        return Result.success();
    }
}
