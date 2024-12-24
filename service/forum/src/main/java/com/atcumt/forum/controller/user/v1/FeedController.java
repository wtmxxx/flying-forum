package com.atcumt.forum.controller.user.v1;

import com.atcumt.model.common.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("feedControllerV1")
@RequestMapping("/api/forum/feed/v1")
@Tag(name = "Feed", description = "内容流相关接口")
@RequiredArgsConstructor
@Slf4j
public class FeedController {
    @GetMapping("/test")
    @Operation(summary = "获取用户权限", description = "获取指定用户的权限信息")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "userId", description = "用户ID", required = true)
    })
    public Result<Object> test() {
        return Result.success();
    }
}
