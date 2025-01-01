package com.atcumt.comment.controller.user.v1;

import com.atcumt.comment.service.ReplyService;
import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.comment.dto.CommentReplyDTO;
import com.atcumt.model.comment.dto.ReplyDTO;
import com.atcumt.model.comment.dto.UserReplyDTO;
import com.atcumt.model.comment.vo.CommentReplyVO;
import com.atcumt.model.comment.vo.ReplyVO;
import com.atcumt.model.comment.vo.UserReplyVO;
import com.atcumt.model.common.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("replyControllerV1")
@RequestMapping("/api/reply/v1")
@Tag(name = "Reply", description = "回复相关接口")
@RequiredArgsConstructor
@Slf4j
public class ReplyController {
    private final ReplyService replyService;

    @PostMapping("")
    @Operation(summary = "发表回复", description = "发表回复")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<ReplyVO> postReply(@RequestBody ReplyDTO replyDTO) throws AuthorizationException {
        log.info("发表回复, userId: {}", UserContext.getUserId());

        ReplyVO replyVO = replyService.postReply(replyDTO);

        return Result.success(replyVO);
    }

    @GetMapping("/{replyId}")
    @Operation(summary = "获取回复", description = "获取回复")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "replyId", description = "回复ID", in = ParameterIn.PATH, required = true)
    })
    public Result<ReplyVO> getReply(@PathVariable("replyId") Long replyId) throws AuthorizationException {
        log.info("获取回复, userId: {}", UserContext.getUserId());

        ReplyVO replyVO = replyService.getReply(replyId);

        return Result.success(replyVO);
    }

    @DeleteMapping("/{replyId}")
    @Operation(summary = "删除回复", description = "删除回复")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "replyId", description = "回复ID", in = ParameterIn.PATH, required = true)
    })
    public Result<Object> deleteReply(@PathVariable("replyId") Long replyId) throws AuthorizationException {
        log.info("删除回复, userId: {}", UserContext.getUserId());

        replyService.deleteReply(replyId);

        return Result.success();
    }

    @GetMapping("/comment/{commentId}")
    @Operation(summary = "获取评论回复", description = "获取评论回复")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "commentId", description = "评论ID", in = ParameterIn.PATH, required = true),
            @Parameter(name = "cursor", description = "游标", in = ParameterIn.QUERY),
            @Parameter(name = "lastReplyId", description = "最后一条回复ID", in = ParameterIn.QUERY),
            @Parameter(name = "size", description = "数量", in = ParameterIn.QUERY)
    })
    public Result<CommentReplyVO> getPostComments(
            @PathVariable("commentId") Long commentId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "lastReplyId", required = false) Long lastReplyId,
            @RequestParam(name = "size", defaultValue = "10") Integer size
    ) throws AuthorizationException {
        log.info("获取评论回复, userId: {}", UserContext.getUserId());

        if (size > 1000 || size < 0) size = 10;

        CommentReplyDTO commentReplyDTO = CommentReplyDTO.builder()
                .commentId(commentId)
                .cursor(cursor)
                .lastReplyId(lastReplyId)
                .size(size)
                .build();

        CommentReplyVO commentReplyVO = replyService.getCommentReplies(commentReplyDTO);

        return Result.success(commentReplyVO);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户回复", description = "获取用户回复")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "cursor", description = "游标", in = ParameterIn.QUERY),
            @Parameter(name = "lastReplyId", description = "最后一条回复ID", in = ParameterIn.QUERY),
            @Parameter(name = "size", description = "数量", in = ParameterIn.QUERY),
            @Parameter(name = "userId", description = "用户ID", in = ParameterIn.PATH, required = true)
    })
    public Result<UserReplyVO> getUserComments(
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "lastReplyId", required = false) Long lastReplyId,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @PathVariable(name = "userId") String userId
    ) throws AuthorizationException {
        log.info("获取用户回复, userId: {}", UserContext.getUserId());

        if (size > 1000 || size < 0) size = 10;

        UserReplyDTO userReplyDTO = UserReplyDTO.builder()
                .userId(userId)
                .cursor(cursor)
                .lastReplyId(lastReplyId)
                .size(size)
                .build();

        UserReplyVO userReplyVO = replyService.getUserReplies(userReplyDTO);

        return Result.success(userReplyVO);
    }
}
