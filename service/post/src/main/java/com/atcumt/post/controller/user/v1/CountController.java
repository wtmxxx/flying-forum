package com.atcumt.post.controller.user.v1;

import com.atcumt.model.common.entity.Result;
import com.atcumt.model.post.dto.PostViewCountDTO;
import com.atcumt.post.service.CountService;
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

@RestController("countControllerV1")
@RequestMapping("/api/post/count/v1")
@Tag(name = "Count", description = "计数相关接口")
@RequiredArgsConstructor
@Slf4j
public class CountController {
    private final CountService countService;

    @PostMapping("/post")
    @Operation(summary = "帖子浏览量计数", description = "帖子浏览量计数，请求一次+1")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> incrPostViewCount(@RequestBody PostViewCountDTO postViewCountDTO) {
        log.info("帖子浏览量计数, postId: {}", postViewCountDTO.getPostId());

        countService.incrPostViewCount(postViewCountDTO);

        return Result.success();
    }
}
