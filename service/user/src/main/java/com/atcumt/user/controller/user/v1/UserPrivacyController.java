package com.atcumt.user.controller.user.v1;

import com.atcumt.model.common.entity.Result;
import com.atcumt.model.user.dto.UserPrivacyDTO;
import com.atcumt.model.user.vo.UserPrivacyVO;
import com.atcumt.user.service.UserPrivacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("userPrivacyControllerUserV1")
@RequestMapping("/api/user/privacy/v1")
@Tag(name = "UserPrivacy", description = "用户隐私相关接口")
@RequiredArgsConstructor
@Slf4j
public class UserPrivacyController {
    private final UserPrivacyService userPrivacyService;

    @PatchMapping("")
    @Operation(summary = "设置隐私级别", description = "设置隐私级别(主页部件可见性)")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> setPrivacyLevel(@RequestBody UserPrivacyDTO userPrivacyDTO) {
        log.info("设置隐私级别, privacyScope: {}, privacyLevel: {}", userPrivacyDTO.getPrivacyScope(), userPrivacyDTO.getPrivacyLevel());

        userPrivacyService.setPrivacyLevel(userPrivacyDTO);

        return Result.success();
    }

    @GetMapping("/{privacyScope}")
    @Operation(summary = "获取隐私级别", description = "获取隐私级别(主页部件可见性)")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "privacyScope", description = "隐私范围", in = ParameterIn.PATH, required = true)
    })
    public Result<UserPrivacyVO> getPrivacyLevel(@PathVariable String privacyScope) {
        log.info("获取隐私级别, privacyScope: {}", privacyScope);

        UserPrivacyVO userPrivacyVO = userPrivacyService.getPrivacyLevel(privacyScope);

        return Result.success(userPrivacyVO);
    }
}
