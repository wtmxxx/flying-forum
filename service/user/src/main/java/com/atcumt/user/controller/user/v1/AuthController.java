package com.atcumt.user.controller.user.v1;

import com.atcumt.model.common.AuthMessage;
import com.atcumt.model.common.Result;
import com.atcumt.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("gptControllerV2")
@RequestMapping("/api/v1/user/user")
@Tag(name = "User", description = "用户相关接口")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register/stu_id")
    @Operation(summary = "用户注册")
    @Parameters({
            @Parameter(name = "studentId", description = "学号", required = true),
            @Parameter(name = "unifiedPassword", description = "统一身份认证密码", required = true)
    })
    public Result<String> registerByStuId(String studentId, String unifiedPassword) throws Exception {
        log.info("用户注册, studentId: {}", studentId);

        String jwt = authService.registerByStuId(studentId, unifiedPassword);

        if (jwt != null) {
            // 返回JWT Token
            return Result.success(jwt);
        } else {
            // 系统错误
            return Result.failure(AuthMessage.SYSTEM_ERROR.getMessage());
        }
    }
}
