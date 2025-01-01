package com.atcumt.comment.controller.user.v1;

import com.atcumt.comment.service.CommentService;
import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.comment.dto.CommentDTO;
import com.atcumt.model.comment.dto.PostCommentDTO;
import com.atcumt.model.comment.dto.UserCommentDTO;
import com.atcumt.model.comment.vo.CommentVO;
import com.atcumt.model.comment.vo.PostCommentVO;
import com.atcumt.model.comment.vo.UserCommentVO;
import com.atcumt.model.common.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("commentControllerV1")
@RequestMapping("/api/comment/v1")
@Tag(name = "Comment", description = "评论相关接口")
@RequiredArgsConstructor
@Slf4j
public class CommentController {
    private final CommentService commentService;

    @PostMapping("")
    @Operation(summary = "发表评论", description = "发表评论")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<CommentVO> postComment(@RequestBody CommentDTO commentDTO) throws AuthorizationException {
        log.info("发表评论, userId: {}", UserContext.getUserId());

        CommentVO commentVO = commentService.postComment(commentDTO);

        return Result.success(commentVO);
    }

    @GetMapping("/{commentId}")
    @Operation(summary = "获取评论", description = "获取评论")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "commentId", description = "评论ID", in = ParameterIn.PATH, required = true)
    })
    public Result<CommentVO> getComment(@PathVariable("commentId") Long commentId) throws AuthorizationException {
        log.info("获取评论, userId: {}", UserContext.getUserId());

        CommentVO commentVO = commentService.getComment(commentId);

        return Result.success(commentVO);
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "删除评论", description = "删除评论")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "commentId", description = "评论ID", in = ParameterIn.PATH, required = true)
    })
    public Result<Object> deleteComment(@PathVariable("commentId") Long commentId) throws AuthorizationException {
        log.info("删除评论, userId: {}", UserContext.getUserId());

        commentService.deleteComment(commentId);

        return Result.success();
    }

    @GetMapping("/post/{postType}/{postId}")
    @Operation(summary = "获取帖子评论", description = "获取帖子评论")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "postType", description = "帖子类型", in = ParameterIn.PATH, required = true),
            @Parameter(name = "postId", description = "帖子ID", in = ParameterIn.PATH, required = true),
            @Parameter(name = "cursor", description = "游标", in = ParameterIn.QUERY),
            @Parameter(name = "lastCommentId", description = "最后一条评论ID", in = ParameterIn.QUERY),
            @Parameter(name = "size", description = "数量", in = ParameterIn.QUERY),
            @Parameter(name = "sort", description = "排序", in = ParameterIn.QUERY)
    })
    public Result<PostCommentVO> getPostComments(
            @PathVariable("postType") String postType,
            @PathVariable("postId") Long postId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "lastCommentId", required = false) Long lastCommentId,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestParam(name = "sort", defaultValue = "score") String sort
    ) throws AuthorizationException {
        log.info("获取帖子评论, userId: {}", UserContext.getUserId());

        if (size > 1000 || size < 0) size = 10;

        PostCommentDTO postCommentDTO = PostCommentDTO.builder()
                .postType(postType)
                .postId(postId)
                .cursor(cursor)
                .lastCommentId(lastCommentId)
                .size(size)
                .sort(sort)
                .build();

        PostCommentVO postCommentVO = commentService.getPostComments(postCommentDTO);

        return Result.success(postCommentVO);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户评论", description = "获取用户评论")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "cursor", description = "游标", in = ParameterIn.QUERY),
            @Parameter(name = "lastCommentId", description = "最后一条评论ID", in = ParameterIn.QUERY),
            @Parameter(name = "size", description = "数量", in = ParameterIn.QUERY),
            @Parameter(name = "userId", description = "用户ID", in = ParameterIn.PATH, required = true)
    })
    public Result<UserCommentVO> getUserComments(
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "lastCommentId", required = false) Long lastCommentId,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @PathVariable(name = "userId") String userId
    ) throws AuthorizationException {
        log.info("获取我的评论, userId: {}", UserContext.getUserId());

        if (size > 1000 || size < 0) size = 10;

        UserCommentDTO userCommentDTO = UserCommentDTO.builder()
                .userId(userId)
                .cursor(cursor)
                .lastCommentId(lastCommentId)
                .size(size)
                .build();

        UserCommentVO userCommentVO = commentService.getUserComments(userCommentDTO);

        return Result.success(userCommentVO);
    }
}
