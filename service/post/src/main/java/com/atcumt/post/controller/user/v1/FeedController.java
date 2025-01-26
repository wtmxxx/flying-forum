package com.atcumt.post.controller.user.v1;

import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.post.dto.PostFeedDTO;
import com.atcumt.model.post.vo.PostFeedListVO;
import com.atcumt.post.service.PostService;
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

@RestController("feedControllerV1")
@RequestMapping("/api/post/feed/v1")
@Tag(name = "Feed", description = "内容流相关接口")
@RequiredArgsConstructor
@Slf4j
public class FeedController {
    private final PostService postService;

    @PostMapping("/post")
    @Operation(summary = "获取帖子", description = "获取帖子")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<PostFeedListVO> getPostComments(@RequestBody PostFeedDTO postFeedDTO) throws AuthorizationException {
        log.info("获取帖子, userId: {}", UserContext.getUserId());

        if (postFeedDTO.getSize() > 1000 || postFeedDTO.getSize() < 0) postFeedDTO.setSize(10);

        PostFeedListVO postFeedListVO = postService.getPosts(postFeedDTO);

        return Result.success(postFeedListVO);
    }
}
