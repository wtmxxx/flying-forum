package com.atcumt.auth.controller.admin.v1;

import com.atcumt.auth.service.DisableService;
import com.atcumt.model.auth.dto.DisableServiceBatchDTO;
import com.atcumt.model.auth.dto.UntieDisableServiceBatchDTO;
import com.atcumt.model.auth.vo.DisableTimeVO;
import com.atcumt.model.common.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController("disableControllerAdminV1")
@RequestMapping("/api/auth/admin/disable/v1")
@Tag(name = "Disable", description = "封禁管理相关接口")
@RequiredArgsConstructor
@Slf4j
public class DisableController {
    private final DisableService disableService;

    @PutMapping("/disable/{service}")
    @Operation(summary = "封禁单个账号或服务", description = "封禁单个指定账号或服务")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "userId", description = "用户ID", required = true),
            @Parameter(name = "service", description = "服务名称", in = ParameterIn.PATH, required = true),
            @Parameter(name = "duration", description = "封禁时长(s)", required = true)
    })
    public Result<Object> disableService(String userId, @PathVariable("service") String service, Long duration) {
        log.info("封禁账号或服务, userId: {}", userId);

        // 权限鉴定
        // 放在Service里面鉴定

        disableService.disableService(userId, service, duration);

        return Result.success();
    }

    @PutMapping("/disable")
    @Operation(summary = "批量封禁账号或服务", description = "批量封禁指定账号或服务")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> disableServiceBatch(@RequestBody DisableServiceBatchDTO disableServiceBatchDTO) {
        log.info("批量封禁账号或服务");

        disableService.disableServiceBatch(disableServiceBatchDTO);

        return Result.success();
    }

    @PutMapping("/untie-disable/{service}")
    @Operation(summary = "解封账号或服务", description = "解封指定账号或服务")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "userId", description = "用户ID", required = true),
            @Parameter(name = "service", description = "服务名称", in = ParameterIn.PATH, required = true)
    })
    public Result<Object> untieDisableService(String userId, @PathVariable("service") String service) {
        log.info("解封账号或服务");

        disableService.untieDisableService(userId, service);

        return Result.success();
    }

    @PutMapping("/untie-disable")
    @Operation(summary = "批量解封账号或服务", description = "批量解封指定账号或服务")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> untieDisableServiceBatch(@RequestBody UntieDisableServiceBatchDTO untieDisableServiceBatchDTO) {
        log.info("批量解封账号或服务");

        disableService.untieDisableServiceBatch(untieDisableServiceBatchDTO);

        return Result.success();
    }

    @GetMapping("/service")
    @Operation(summary = "获取封禁服务列表", description = "获取目前可以被封禁的服务列表")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<List<String>> getDisableService() {
        log.info("获取封禁服务列表");

        List<String> services = disableService.getDisableService();

        return Result.success(services);
    }

    @GetMapping("/service/time")
    @Operation(summary = "获取用户所有被封禁服务时长", description = "获取用户所有被封禁服务时长，-3：无权限，-2：未被封禁，-1：永久封禁")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "userId", description = "用户ID", required = true)
    })
    public Result<DisableTimeVO> getAllDisableService(String userId) {
        log.info("获取用户所有被封禁服务时长, userId: {}", userId);

        DisableTimeVO services = disableService.getAllDisableServiceTimes(userId);

        return Result.success(services);
    }

    @GetMapping("/{service}/time")
    @Operation(summary = "获取用户被封禁服务时长", description = "获取指定用户被封禁服务时长，-3：无权限，-2：未被封禁，-1：永久封禁")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "userId", description = "用户ID", required = true),
            @Parameter(name = "service", description = "服务名称", in = ParameterIn.PATH, required = true)
    })
    public Result<DisableTimeVO> getDisableService(String userId, @PathVariable("service") String service) {
        log.info("获取用户被封禁服务时长, userId: {}, service: {}", userId, service);

        DisableTimeVO services = disableService.getDisableServiceTime(userId, service);

        return Result.success(services);
    }
}
