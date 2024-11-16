package com.atcumt.auth.controller.admin.v1;

import com.atcumt.auth.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("authControllerAdminV1")
@RequestMapping("/api/auth/admin/v1")
@Tag(name = "Auth", description = "管理员鉴权相关接口")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

}
