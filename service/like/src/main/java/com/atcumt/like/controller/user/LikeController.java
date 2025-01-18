package com.atcumt.like.controller.user;

import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.common.utils.UserContext;
import com.atcumt.like.service.LikeService;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.like.dto.CommentLikeDTO;
import com.atcumt.model.like.dto.PostLikeDTO;
import com.atcumt.model.like.dto.UserLikeDTO;
import com.atcumt.model.like.vo.UserPostLikeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("likeControllerV1")
@RequestMapping("/api/like/v1")
@Tag(name = "Like", description = "点赞相关接口")
@RequiredArgsConstructor
@Slf4j
public class LikeController {
    private final LikeService likeService;

    @PostMapping("/{action}/post/{postType}/{postId}")
    @Operation(summary = "点赞帖子", description = "点赞帖子")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "action", description = "点赞/点踩/取消, like/dislike/cancel", in = ParameterIn.PATH, example = "cancel", required = true),
            @Parameter(name = "postType", description = "帖子类型, discussion,etc.", in = ParameterIn.PATH, example = "discussion", required = true),
            @Parameter(name = "postId", description = "帖子ID", in = ParameterIn.PATH, required = true)
    })
    public Result<Object> likePost(
            @PathVariable("action") String action,
            @PathVariable("postType") String postType,
            @PathVariable("postId") Long postId
    ) throws AuthorizationException {
        log.info("点赞帖子, userId: {}", UserContext.getUserId());

        PostLikeDTO postLikeDTO = PostLikeDTO
                .builder()
                .action(action)
                .postType(postType)
                .postId(postId)
                .userId(UserContext.getUserId())
                .build();

        likeService.likePost(postLikeDTO);

        return Result.success();
    }

    @PostMapping("/{action}/comment/{commentType}/{commentId}")
    @Operation(summary = "点赞评论", description = "点赞评论")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "action", description = "点赞/点踩/取消, like/dislike/cancel", in = ParameterIn.PATH, example = "cancel", required = true),
            @Parameter(name = "commentType", description = "评论类型, comment/reply", in = ParameterIn.PATH, example = "reply", required = true),
            @Parameter(name = "commentId", description = "评论ID", in = ParameterIn.PATH, required = true)
    })
    public Result<Object> likeComment(
            @PathVariable("action") String action,
            @PathVariable("commentType") String commentType,
            @PathVariable("commentId") Long commentId
    ) throws AuthorizationException {
        log.info("点赞评论, userId: {}", UserContext.getUserId());

        CommentLikeDTO commentLikeDTO = CommentLikeDTO
                .builder()
                .action(action)
                .commentType(commentType)
                .commentId(commentId)
                .userId(UserContext.getUserId())
                .build();

        likeService.likeComment(commentLikeDTO);

        return Result.success();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户点赞列表", description = "获取用户点赞列表")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "cursor", description = "游标", in = ParameterIn.QUERY),
            @Parameter(name = "lastLikeId", description = "最后一条评论ID", in = ParameterIn.QUERY),
            @Parameter(name = "size", description = "数量", in = ParameterIn.QUERY),
            @Parameter(name = "userId", description = "用户ID", in = ParameterIn.PATH, required = true)
    })
    public Result<UserPostLikeVO> getLikeList(
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "lastLikeId", required = false) Long lastLikeId,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @PathVariable(name = "userId") String userId
    ) throws InterruptedException {
        log.info("获取用户点赞列表, userId: {}", userId);

        if (size > 1000 || size < 0) size = 10;

        UserLikeDTO userLikeDTO = UserLikeDTO.builder()
                .userId(userId)
                .cursor(cursor)
                .lastLikeId(lastLikeId)
                .size(size)
                .build();

        UserPostLikeVO userPostLikeVO = likeService.getUserLikes(userLikeDTO);

        return Result.success(userPostLikeVO);
    }
}
