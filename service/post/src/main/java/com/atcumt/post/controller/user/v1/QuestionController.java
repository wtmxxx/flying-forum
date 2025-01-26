package com.atcumt.post.controller.user.v1;

import cn.dev33.satoken.annotation.SaCheckDisable;
import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.post.dto.QuestionDTO;
import com.atcumt.model.post.dto.QuestionUpdateDTO;
import com.atcumt.model.post.vo.QuestionPostVO;
import com.atcumt.model.post.vo.QuestionVO;
import com.atcumt.post.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("questionControllerV1")
@RequestMapping("/api/post/question/v1")
@Tag(name = "Question", description = "问答相关接口")
@RequiredArgsConstructor
@Slf4j
public class QuestionController {
    private final QuestionService questionService;

    @PostMapping("")
    @Operation(summary = "发表问答", description = "发表问答")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    @SaCheckDisable("question")
    public Result<QuestionPostVO> postQuestion(@RequestBody QuestionDTO questionDTO) throws Exception {
        log.info("发表问答, authorId: {}", UserContext.getUserId());

        QuestionPostVO questionPostVO = questionService.postQuestion(questionDTO);

        return Result.success(questionPostVO);
    }

    @PostMapping("/draft")
    @Operation(summary = "保存问答草稿", description = "保存问答草稿")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<QuestionPostVO> saveQuestionAsDraft(@RequestBody QuestionDTO questionDTO) {
        log.info("保存问答草稿, authorId: {}", UserContext.getUserId());

        QuestionPostVO questionPostVO = questionService.saveQuestionAsDraft(questionDTO);

        return Result.success(questionPostVO);
    }

    @PatchMapping("")
    @Operation(summary = "修改问答", description = "修改问答")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    @SaCheckDisable("question")
    public Result<QuestionPostVO> updateQuestion(@RequestBody QuestionUpdateDTO questionUpdateDTO) throws AuthorizationException {
        log.info("修改问答, authorId: {}", UserContext.getUserId());

        QuestionPostVO questionPostVO = questionService.updateQuestion(questionUpdateDTO);

        return Result.success(questionPostVO);
    }

    @DeleteMapping("")
    @Operation(summary = "删除问答", description = "删除问答")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "questionId", description = "问答ID", required = true)
    })
    public Result<Object> deleteQuestion(Long questionId) throws AuthorizationException {
        log.info("删除问答, authorId: {}", UserContext.getUserId());

        // 删除帖子
        questionService.deleteQuestion(questionId);

        return Result.success();
    }

    @PatchMapping("/private")
    @Operation(summary = "私密问答", description = "私密问答")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "questionId", description = "问答ID", required = true)
    })
    public Result<Object> privateQuestion(Long questionId) throws AuthorizationException {
        log.info("私密问答, authorId: {}", UserContext.getUserId());

        // 私密帖子
        questionService.privateQuestion(questionId);

        return Result.success();
    }

    @GetMapping("")
    @Operation(summary = "获取问答", description = "获取问答")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "questionId", description = "问答ID", in = ParameterIn.QUERY, required = true)
    })
    public Result<QuestionVO> getQuestion(Long questionId) {
        log.info("获取问答, questionId: {}", questionId);

        // 获取问答
        QuestionVO questionVO = questionService.getQuestion(questionId);

        return Result.success(questionVO);
    }
}
