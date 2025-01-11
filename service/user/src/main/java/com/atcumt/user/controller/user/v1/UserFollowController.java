package com.atcumt.user.controller.user.v1;

import com.atcumt.common.utils.UserContext;
import com.atcumt.model.common.entity.Result;
import com.atcumt.user.service.UserFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController("userFollowControllerUserV1")
@RequestMapping("/api/user/follow/v1")
@Tag(name = "UserFollow", description = "用户关注相关接口")
@RequiredArgsConstructor
@Slf4j
public class UserFollowController {
    private final UserFollowService userFollowService;

    @PostMapping("/user/{followedId}")
    @Operation(summary = "关注", description = "关注/取消关注用户")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "followedId", description = "用户ID", in = ParameterIn.PATH, required = true)
    })
    public Result<Object> followUser(@PathVariable String followedId) {
        log.info("用户 {} 关注用户 {}", UserContext.getUserId(), followedId);

        userFollowService.followUser(followedId);

        return Result.success();
    }
}
