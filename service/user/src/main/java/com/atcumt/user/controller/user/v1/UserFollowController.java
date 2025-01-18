package com.atcumt.user.controller.user.v1;

import com.atcumt.common.utils.UserContext;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.user.dto.UserFollowerDTO;
import com.atcumt.model.user.dto.UserFollowingDTO;
import com.atcumt.model.user.vo.UserFollowerVO;
import com.atcumt.model.user.vo.UserFollowingVO;
import com.atcumt.user.service.UserFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/followings/user/{userId}")
    @Operation(summary = "获取用户关注列表", description = "获取用户关注列表")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "cursor", description = "游标", in = ParameterIn.QUERY),
            @Parameter(name = "lastFollowId", description = "最后一条关注ID", in = ParameterIn.QUERY),
            @Parameter(name = "size", description = "数量", in = ParameterIn.QUERY),
            @Parameter(name = "userId", description = "用户ID", in = ParameterIn.PATH, required = true)
    })
    public Result<UserFollowingVO> getFollowingList(
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "lastFollowId", required = false) Long lastFollowId,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @PathVariable(name = "userId") String userId
    ) throws InterruptedException {
        log.info("获取用户关注列表, userId: {}", userId);

        if (size > 1000 || size < 0) size = 10;

        UserFollowingDTO userFollowingDTO = UserFollowingDTO.builder()
                .userId(userId)
                .cursor(cursor)
                .lastFollowId(lastFollowId)
                .size(size)
                .build();

        UserFollowingVO userFollowingVO = userFollowService.getUserFollowings(userFollowingDTO);

        return Result.success(userFollowingVO);
    }

    @GetMapping("/followers/user/{userId}")
    @Operation(summary = "获取用户粉丝列表", description = "获取用户粉丝列表")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "cursor", description = "游标", in = ParameterIn.QUERY),
            @Parameter(name = "lastFollowId", description = "最后一条粉丝ID", in = ParameterIn.QUERY),
            @Parameter(name = "size", description = "数量", in = ParameterIn.QUERY),
            @Parameter(name = "userId", description = "用户ID", in = ParameterIn.PATH, required = true)
    })
    public Result<UserFollowerVO> getFollowerList(
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "lastFollowId", required = false) Long lastFollowId,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @PathVariable(name = "userId") String userId
    ) throws InterruptedException {
        log.info("获取用户粉丝列表, userId: {}", userId);

        if (size > 1000 || size < 0) size = 10;

        UserFollowerDTO userFollowerDTO = UserFollowerDTO.builder()
                .userId(userId)
                .cursor(cursor)
                .lastFollowId(lastFollowId)
                .size(size)
                .build();

        UserFollowerVO userFollowerVO = userFollowService.getUserFollowers(userFollowerDTO);

        return Result.success(userFollowerVO);
    }
}
