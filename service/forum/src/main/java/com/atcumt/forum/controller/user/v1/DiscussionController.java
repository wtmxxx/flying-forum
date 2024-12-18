package com.atcumt.forum.controller.user.v1;

import cn.dev33.satoken.annotation.SaCheckDisable;
import com.atcumt.common.utils.UserContext;
import com.atcumt.forum.service.DiscussionService;
import com.atcumt.model.common.Result;
import com.atcumt.model.forum.dto.DiscussionDTO;
import com.atcumt.model.forum.dto.DiscussionUpdateDTO;
import com.atcumt.model.forum.vo.DiscussionPostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("discussionControllerV1")
@RequestMapping("/api/forum/discussion/v1")
@Tag(name = "Discussion", description = "杂谈相关接口")
@RequiredArgsConstructor
@Slf4j
public class DiscussionController {
    private final DiscussionService discussionService;

    @PostMapping("/")
    @Operation(summary = "发表杂谈", description = "发表杂谈")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    @SaCheckDisable("discussion")
    public Result<DiscussionPostVO> postDiscussion(@RequestBody DiscussionDTO discussionDTO) throws Exception {
        log.info("发表杂谈, authorId: {}", UserContext.getUserId());

        DiscussionPostVO discussionPostVO = discussionService.postDiscussion(discussionDTO);

        return Result.success(discussionPostVO);
    }

    @PostMapping("/draft")
    @Operation(summary = "保存杂谈草稿", description = "保存杂谈草稿")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<DiscussionPostVO> saveDiscussionAsDraft(@RequestBody DiscussionDTO discussionDTO) {
        log.info("保存杂谈草稿, authorId: {}", UserContext.getUserId());

        DiscussionPostVO discussionPostVO = discussionService.saveDiscussionAsDraft(discussionDTO);

        return Result.success(discussionPostVO);
    }

    @PatchMapping("/")
    @Operation(summary = "修改杂谈", description = "修改杂谈")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    @SaCheckDisable("discussion")
    public Result<DiscussionPostVO> updateDiscussion(@RequestBody DiscussionUpdateDTO discussionUpdateDTO) {
        log.info("修改杂谈, authorId: {}", UserContext.getUserId());

        DiscussionPostVO discussionPostVO = discussionService.updateDiscussion(discussionUpdateDTO);

        return Result.success(discussionPostVO);
    }

    @DeleteMapping("/")
    @Operation(summary = "删除杂谈", description = "删除杂谈")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "discussionId", description = "杂谈ID", required = true)
    })
    public Result<Object> deleteDiscussion(Long discussionId) {
        log.info("删除杂谈, authorId: {}", UserContext.getUserId());

        // 删除帖子
        discussionService.deleteDiscussion(discussionId);

        return Result.success();
    }

    @PatchMapping("/private")
    @Operation(summary = "私密杂谈", description = "私密杂谈")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "discussionId", description = "杂谈ID", required = true)
    })
    public Result<Object> privateDiscussion(Long discussionId) {
        log.info("私密杂谈, authorId: {}", UserContext.getUserId());

        // 私密帖子
        discussionService.privateDiscussion(discussionId);

        return Result.success();
    }

    @GetMapping("/")
    @Operation(summary = "获取杂谈", description = "获取杂谈")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> getDiscussion(Long discussionId) {
        log.info("获取杂谈, authorId: {}", discussionId);

        // TODO 获取杂谈

        return Result.success();
    }
}
