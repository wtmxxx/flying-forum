package com.atcumt.auth.controller.admin.v1;

import cn.dev33.satoken.stp.StpUtil;
import com.atcumt.auth.service.RoleService;
import com.atcumt.common.enums.PermAction;
import com.atcumt.common.enums.PermModule;
import com.atcumt.common.utils.PermissionUtil;
import com.atcumt.model.auth.dto.RoleDTO;
import com.atcumt.model.auth.dto.UserRoleDTO;
import com.atcumt.model.auth.vo.RoleVO;
import com.atcumt.model.common.AuthMessage;
import com.atcumt.model.common.PageQueryDTO;
import com.atcumt.model.common.PageQueryVO;
import com.atcumt.model.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController("roleControllerV1")
@RequestMapping("/api/admin/role/v1")
@Tag(name = "Role", description = "角色管理相关接口")
@RequiredArgsConstructor
@Slf4j
public class RoleController {
    private final RoleService roleService;

    @GetMapping("/user")
    @Operation(summary = "获取用户角色", description = "获取用户角色信息，如果传入userId，则需要鉴定权限，可以返回他人角色信息，否则仅返回登录账号角色信息")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "userId", description = "用户ID"),
    })
    public Result<List<RoleVO>> getRole(String userId) {
        log.info("获取用户角色");

        List<RoleVO> roleVOs = roleService.getUserRole(userId);

        if (roleVOs != null && !roleVOs.isEmpty()) {
            // 返回 roleVO
            return Result.success(roleVOs);
        } else {
            // 系统错误
            return Result.failure(AuthMessage.SYSTEM_ERROR.getMessage());
        }
    }

    @PostMapping("/user")
    @Operation(summary = "新增单个指定用户角色", description = "新增单个指定用户的角色信息")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "userId", description = "用户ID", required = true),
            @Parameter(name = "roleId", description = "角色ID", required = true)
    })
    public Result<Object> updateRolePermission(@RequestParam String userId, @RequestParam String roleId) {
        log.info("新增单个指定角色权限, userId: {}", userId);

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.USER_ROLE, PermAction.UPDATE));

        roleService.updateUserRole(userId, roleId);

        return Result.success();
    }

    @PutMapping("/user")
    @Operation(summary = "修改用户角色", description = "修改用户角色信息，修改自己或他人，全量替换")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> setUserRole(@RequestBody UserRoleDTO userRoleDTO) {
        log.info("修改用户角色");

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.USER_ROLE, PermAction.UPDATE));

        roleService.updateUserRoles(userRoleDTO);

        return Result.success();
    }

    @DeleteMapping("/user")
    @Operation(summary = "删除单个用户角色", description = "删除单个用户角色信息，删除自己或他人")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "userId", description = "用户ID"),
            @Parameter(name = "roleId", description = "角色ID")
    })
    public Result<Object> deleteUserRole(String userId, String roleId) {
        log.info("删除用户角色");

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.USER_ROLE, PermAction.DELETE));

        roleService.deleteUserRole(
                UserRoleDTO
                        .builder()
                        .userId(userId)
                        .roleIds(List.of(roleId))
                        .build()
        );

        return Result.success();
    }

    @PatchMapping("/user/batch-delete")
    @Operation(summary = "批量删除用户角色", description = "批量删除用户角色信息，删除自己或他人")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> deleteUserRole(@RequestBody UserRoleDTO userRoleDTO) {
        log.info("批量删除用户角色");

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.USER_ROLE, PermAction.DELETE));

        roleService.deleteUserRole(userRoleDTO);

        return Result.success();
    }

    @GetMapping("/all")
    @Operation(summary = "获取分页角色", description = "获取分页角色信息")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "page", description = "页码", example = "1", required = true),
            @Parameter(name = "size", description = "每页的记录数", example = "10", required = true)
    })
    public Result<Object> getAllRole(Long page, Long size) {
        log.info("获取分页角色标题, page: {}, size: {}", page, size);

        PageQueryDTO pageQueryDTO = PageQueryDTO
                .builder()
                .page(page)
                .size(size)
                .build();

        // 分页查询参数校验
        pageQueryDTO.checkParam();

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.ROLE, PermAction.READ));

        PageQueryVO<RoleVO> pageQueryVO = roleService.getAllRole(pageQueryDTO);

        return Result.success(pageQueryVO);
    }

    @PostMapping("/new")
    @Operation(summary = "新增角色", description = "新增角色")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<RoleVO> createRole(@RequestBody RoleDTO roleDTO) {
        log.info("新增角色, name: {}, description: {}", roleDTO.getRoleName(), roleDTO.getDescription());

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.ROLE, PermAction.CREATE));

        RoleVO roleVO = roleService.createRole(roleDTO);

        return Result.success(roleVO);
    }

    @PutMapping("/description")
    @Operation(summary = "修改角色描述", description = "修改角色描述")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> updateRoleDescription(String roleId, String description) {
        log.info("修改角色描述, id: {}, description: {}", roleId, description);

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.ROLE, PermAction.UPDATE));

        roleService.updateRoleDescription(roleId, description);

        return Result.success();
    }

    @DeleteMapping("/role")
    @Operation(summary = "删除角色", description = "删除角色")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> deleteRole(String roleId) {
        log.info("删除角色, id: {}", roleId);

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.ROLE, PermAction.DELETE));

        roleService.deleteRole(roleId);

        return Result.success();
    }
}