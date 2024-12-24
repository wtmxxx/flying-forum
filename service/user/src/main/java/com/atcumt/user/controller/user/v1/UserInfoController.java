package com.atcumt.user.controller.user.v1;

import com.atcumt.common.utils.UserContext;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.user.vo.UserInfoVO;
import com.atcumt.user.service.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userInfoControllerUserV1")
@RequestMapping("/api/user/info/v1")
@Tag(name = "UserInfo", description = "用户信息相关接口")
@RequiredArgsConstructor
@Slf4j
public class UserInfoController {
    private final UserInfoService userInfoService;

    @GetMapping("/me")
    @Operation(summary = "获取用户信息", description = "获取用户信息")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<UserInfoVO> getUserInfo() {
        log.info("获取用户信息, userId: {}", UserContext.getUserId());

        UserInfoVO userInfoVO = userInfoService.getUserInfo(UserContext.getUserId());

        return Result.success(userInfoVO);
    }

    @GetMapping("/other")
    @Operation(summary = "获取他人信息", description = "获取他人信息")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<UserInfoVO> getUserInfo(String userId) {
        log.info("获取他人信息, userId: {}", userId);

        UserInfoVO userInfoVO = userInfoService.getUserInfo(userId);

        return Result.success(userInfoVO);
    }

    @PatchMapping("/me/nickname")
    @Operation(summary = "修改昵称", description = "修改昵称")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "nickname", description = "昵称", required = true)
    })
    public Result<Object> changeNickname(String nickname) {
        log.info("修改昵称, userId: {}", UserContext.getUserId());

        userInfoService.changeNickname(nickname);

        return Result.success();
    }

    @PatchMapping("/me/avatar")
    @Operation(summary = "修改头像", description = "修改用户头像")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "avatar", description = "头像URL", required = true)
    })
    public Result<Object> changeAvatar(@RequestParam String avatar) {
        log.info("修改头像, userId: {}", UserContext.getUserId());

        userInfoService.changeAvatar(avatar);

        return Result.success();
    }

    @PatchMapping("/me/banner")
    @Operation(summary = "修改横幅", description = "修改用户横幅")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "banner", description = "横幅URL", required = true)
    })
    public Result<Object> changeBanner(@RequestParam String banner) {
        log.info("修改横幅, userId: {}", UserContext.getUserId());

        userInfoService.changeBanner(banner);

        return Result.success();
    }

    @PatchMapping("/me/bio")
    @Operation(summary = "修改简介", description = "修改用户简介")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "bio", description = "简介", required = true)
    })
    public Result<Object> changeBio(@RequestParam String bio) {
        log.info("修改简介, userId: {}", UserContext.getUserId());

        userInfoService.changeBio(bio);

        return Result.success();
    }

    @PatchMapping("/me/gender")
    @Operation(summary = "修改性别", description = "修改用户性别")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "gender", description = "性别", required = true)
    })
    public Result<Object> changeGender(@RequestParam Integer gender) {
        log.info("修改性别, userId: {}", UserContext.getUserId());

        userInfoService.changeGender(gender);

        return Result.success();
    }

    @PatchMapping("/me/status")
    @Operation(summary = "修改状态", description = "修改用户状态")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> changeStatus(@RequestBody List<String> status) {
        log.info("修改状态, userId: {}", UserContext.getUserId());

        userInfoService.changeStatus(status);

        return Result.success();
    }
}
