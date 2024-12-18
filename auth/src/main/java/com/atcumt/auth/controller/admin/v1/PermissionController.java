package com.atcumt.auth.controller.admin.v1;

import cn.dev33.satoken.stp.StpUtil;
import com.atcumt.auth.service.PermissionService;
import com.atcumt.common.enums.PermAction;
import com.atcumt.common.enums.PermModule;
import com.atcumt.common.utils.PermissionUtil;
import com.atcumt.model.auth.dto.PermissionDTO;
import com.atcumt.model.auth.dto.RolePermissionDTO;
import com.atcumt.model.auth.vo.PermissionVO;
import com.atcumt.model.auth.vo.SortedPermissionVO;
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


@RestController("permissionControllerV1")
@RequestMapping("/api/auth/admin/permission/v1")
@Tag(name = "Permission", description = "权限管理相关接口")
@RequiredArgsConstructor
@Slf4j
public class PermissionController {
    private final PermissionService permissionService;

    @GetMapping("/user")
    @Operation(summary = "获取用户权限", description = "获取指定用户的权限信息")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "userId", description = "用户ID", required = true)
    })
    public Result<List<PermissionVO>> getUserPermissions(@RequestParam String userId) {
        log.info("获取用户权限, userId: {}", userId);

        List<PermissionVO> permissionVOs = permissionService.getUserPermissions(userId);

        if (permissionVOs != null && !permissionVOs.isEmpty()) {
            // 返回 roleVO
            return Result.success(permissionVOs);
        } else {
            // 系统错误
            return Result.failure(AuthMessage.SYSTEM_ERROR.getMessage());
        }
    }

    @GetMapping("/role")
    @Operation(summary = "获取角色权限", description = "获取指定角色的权限信息")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "roleId", description = "角色ID", required = true)
    })
    public Result<List<PermissionVO>> getRolePermissions(@RequestParam String roleId) {
        log.info("获取角色权限, roleId: {}", roleId);

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.ROLE_PERMISSION, PermAction.READ));

        List<PermissionVO> permissions = permissionService.getRolePermissions(roleId);

        return Result.success(permissions);
    }

    @PostMapping("/role")
    @Operation(summary = "新增单个指定角色权限", description = "新增单个指定角色的权限信息")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "roleId", description = "角色ID", required = true),
            @Parameter(name = "permissionId", description = "权限ID", required = true),
    })
    public Result<Object> updateRolePermission(@RequestParam String roleId, @RequestParam String permissionId) {
        log.info("新增单个指定角色权限, roleId: {}", roleId);

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.ROLE_PERMISSION, PermAction.UPDATE));

        permissionService.updateRolePermission(roleId, permissionId);

        return Result.success();
    }

    @PutMapping("/role")
    @Operation(summary = "修改角色权限", description = "修改指定角色的权限信息，全量替换")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> updateRolePermissions(@RequestBody RolePermissionDTO rolePermissionDTO) {
        log.info("修改角色权限, roleId: {}", rolePermissionDTO.getRoleId());

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.ROLE_PERMISSION, PermAction.UPDATE));

        permissionService.updateRolePermissions(rolePermissionDTO.getRoleId(), rolePermissionDTO.getPermissionIds());

        return Result.success();
    }

    @DeleteMapping("/role")
    @Operation(summary = "删除单个角色权限", description = "删除指定单个角色的权限信息")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "roleId", description = "角色ID", required = true),
            @Parameter(name = "permissionId", description = "权限ID", required = true)
    })
    public Result<Object> deleteRolePermissions(@RequestParam String roleId, @RequestParam String permissionId) {
        log.info("删除角色权限, roleId: {}", roleId);

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.ROLE_PERMISSION, PermAction.DELETE));

        permissionService.deleteRolePermissions(
                RolePermissionDTO
                        .builder()
                        .roleId(roleId)
                        .permissionIds(List.of(permissionId))
                        .build()
        );

        return Result.success();
    }

    @PatchMapping("/role/batch-delete")
    @Operation(summary = "批量删除指定角色权限", description = "批量删除指定角色权限")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> deleteUserRole(@RequestBody RolePermissionDTO rolePermissionDTO) {
        log.info("批量删除指定角色权限, roleId: {}", rolePermissionDTO.getRoleId());

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.ROLE_PERMISSION, PermAction.DELETE));

        permissionService.deleteRolePermissions(rolePermissionDTO);

        return Result.success();
    }

    @GetMapping("/all")
    @Operation(summary = "获取所有权限", description = "获取所有权限信息，分页展示")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "page", description = "页码", example = "1", required = true),
            @Parameter(name = "size", description = "每页的记录数", example = "10", required = true),
            @Parameter(name = "sorted", description = "是否按模块整理，默认为false", example = "true")
    })
    public Result<Object> getAllPermissions(Long page, Long size, Boolean sorted) {
        log.info("获取所有权限, page: {}, size: {}", page, size);

        PageQueryDTO pageQueryDTO = PageQueryDTO
                .builder()
                .page(page)
                .size(size)
                .build();

        // 分页查询参数校验
        pageQueryDTO.checkParam();

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.PERMISSION, PermAction.READ));

        sorted = (sorted != null && sorted);
        if (sorted) {
            PageQueryVO<SortedPermissionVO> pageQueryVO = permissionService.getAllSortedPermissions(pageQueryDTO);
            return Result.success(pageQueryVO);
        } else {
            PageQueryVO<PermissionVO> pageQueryVO = permissionService.getAllPermissions(pageQueryDTO);
            return Result.success(pageQueryVO);
        }
    }

    @PostMapping("/new")
    @Operation(summary = "新增权限", description = "新增权限，请严格遵守module.action设置权限名称，权限描述请勿包含||或#|#")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<PermissionVO> createPermission(@RequestBody PermissionDTO permissionDTO) {
        log.info("新增权限, name: {}, description: {}", permissionDTO.getPermissionName(), permissionDTO.getDescription());

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.PERMISSION, PermAction.CREATE));

        PermissionVO permissionVO = permissionService.createPermission(permissionDTO);

        return Result.success(permissionVO);
    }

    @PutMapping("/description")
    @Operation(summary = "修改权限描述", description = "修改权限描述")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> updatePermissionDescription(String permissionId, String description) {
        log.info("修改权限描述, id: {}, description: {}", permissionId, description);

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.PERMISSION, PermAction.UPDATE));

        permissionService.updatePermissionDescription(permissionId, description);

        return Result.success();
    }

    @DeleteMapping("/permission")
    @Operation(summary = "删除权限", description = "删除权限")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "permissionId", description = "权限ID", required = true)
    })
    public Result<Object> deletePermission(String permissionId) {
        log.info("删除权限, id: {}", permissionId);

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.PERMISSION, PermAction.DELETE));

        permissionService.deletePermission(permissionId);

        return Result.success();
    }
}
