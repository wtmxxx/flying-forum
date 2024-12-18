package com.atcumt.forum.controller.user.v1;

import com.atcumt.forum.service.LikeService;
import com.atcumt.model.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("likeControllerV1")
@RequestMapping("/api/forum/like/v1")
@Tag(name = "Like", description = "点赞相关接口")
@RequiredArgsConstructor
@Slf4j
public class LikeController {
    private final LikeService likeService;

    @PostMapping("/discussion")
    @Operation(summary = "点赞杂谈", description = "点赞杂谈")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "discussionId", description = "杂谈ID", required = true),
            @Parameter(name = "action", description = "点赞、点踩、取消(1、-1、0)，默认取消")
    })
    public Result<Object> likeDiscussion(Long discussionId, Integer action) {
        if (action == null) action = 0;
        log.info("点赞杂谈, discussionId: {}, action: {}", discussionId, action);

        likeService.likeDiscussion(discussionId, action);

        return Result.success();
    }
}
