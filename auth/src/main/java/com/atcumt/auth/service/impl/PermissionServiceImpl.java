package com.atcumt.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.atcumt.auth.mapper.PermissionMapper;
import com.atcumt.auth.mapper.RoleMapper;
import com.atcumt.auth.mapper.RolePermissionMapper;
import com.atcumt.auth.service.PermissionService;
import com.atcumt.auth.utils.AuthRedisUtil;
import com.atcumt.common.enums.PermAction;
import com.atcumt.common.enums.PermModule;
import com.atcumt.common.utils.PermissionUtil;
import com.atcumt.model.auth.dto.PermissionDTO;
import com.atcumt.model.auth.dto.RolePermissionDTO;
import com.atcumt.model.auth.dto.SortedPermissionDTO;
import com.atcumt.model.auth.entity.Permission;
import com.atcumt.model.auth.entity.RolePermission;
import com.atcumt.model.auth.vo.PermissionVO;
import com.atcumt.model.auth.vo.SortedPermissionVO;
import com.atcumt.model.common.dto.PageQueryDTO;
import com.atcumt.model.common.vo.PageQueryVO;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final AuthRedisUtil authRedisUtil;
    private final RoleMapper roleMapper;

    public PermissionServiceImpl(
            PermissionMapper permissionMapper,
            RolePermissionMapper rolePermissionMapper,
            AuthRedisUtil authRedisUtil,
            RoleMapper roleMapper) {
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.authRedisUtil = authRedisUtil;
        this.roleMapper = roleMapper;
    }

    @Override
    public PageQueryVO<PermissionVO> getAllPermissions(PageQueryDTO pageQueryDTO) {
        // 分页对象
        Page<Permission> permissionPage = Page.of(pageQueryDTO.getPage(), pageQueryDTO.getSize());
        permissionPage.addOrder(OrderItem.desc("permission_name"), OrderItem.asc("update_time"));

        // 查询分页数据
        permissionPage = permissionMapper.selectPage(permissionPage, Wrappers.lambdaQuery());

        // 返回分页结果
        return PageQueryVO
                .<PermissionVO>staticBuilder()
                .totalRecords(permissionPage.getTotal())
                .totalPages(permissionPage.getPages())
                .page(permissionPage.getCurrent())
                .size(permissionPage.getSize())
                .data(BeanUtil.copyToList(permissionPage.getRecords(), PermissionVO.class))
                .build();
    }

    @Override
    public PageQueryVO<SortedPermissionVO> getAllSortedPermissions(PageQueryDTO pageQueryDTO) {
        // 分页对象
        Page<SortedPermissionDTO> permissionPage = Page.of(pageQueryDTO.getPage(), pageQueryDTO.getSize());

        // 查询分页数据
        permissionPage = permissionMapper.selectSortedPermissionsPage(permissionPage);

        List<SortedPermissionVO> modules = new ArrayList<>();

        permissionPage.getRecords().forEach(permissionDTO -> {
            List<PermissionVO> permissionDetails = new ArrayList<>();

            // 按照记录分割（#|#）
            String[] records = permissionDTO.getPermissions().split("#\\|#");

            // 日期格式化
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (String record : records) {
                // 按字段分割（||）
                String[] fields = record.split("\\|\\|");

                if (fields.length == 5) {
                    PermissionVO permissionVO = new PermissionVO();

                    permissionVO.setPermissionId(fields[0]);
                    permissionVO.setPermissionName(fields[1]);
                    permissionVO.setDescription(fields[2]);

                    // 将时间字符串转换为 LocalDateTime
                    permissionVO.setCreateTime(LocalDateTime.parse(fields[3], formatter));
                    permissionVO.setUpdateTime(LocalDateTime.parse(fields[4], formatter));

                    permissionDetails.add(permissionVO);
                }
            }

            modules.add(new SortedPermissionVO(Map.of(permissionDTO.getModule(), permissionDetails)));
        });

        // 返回分页结果
        return PageQueryVO
                .<SortedPermissionVO>staticBuilder()
                .totalRecords(permissionPage.getTotal())
                .totalPages(permissionPage.getPages())
                .page(permissionPage.getCurrent())
                .size(permissionPage.getSize())
                .data(modules)
                .build();
    }

    @Override
    @GlobalTransactional
    public PermissionVO createPermission(PermissionDTO permissionDTO) {
        String namePattern = "^[a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)*$";
        if (!permissionDTO.getPermissionName().matches(namePattern)) {
            throw new IllegalArgumentException("权限名称不合法，请严格遵守module.action格式");
        }
        // 检查权限描述是否合法
        String descriptionPattern = "^(?!.*\\|\\|)(?!.*#\\|#).*$";
        if (!permissionDTO.getDescription().matches(descriptionPattern)) {
            throw new IllegalArgumentException("权限描述不合法，不能包含||或#|#");
        }

        // 检查是否已存在相同名称的权限
        Permission permission = permissionMapper.selectOne(Wrappers
                .<Permission>lambdaQuery()
                .eq(Permission::getPermissionName, permissionDTO.getPermissionName())
        );

        if (permission != null) {
            throw new IllegalArgumentException("权限已存在");
        }

        // 创建新的权限对象并插入数据库
        permission = Permission
                .builder()
                .permissionName(permissionDTO.getPermissionName())
                .description(permissionDTO.getDescription())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        permissionMapper.insert(permission);

        return BeanUtil.toBean(permission, PermissionVO.class);
    }

    @Override
    @GlobalTransactional
    public void updatePermissionDescription(String permissionId, String description) {
        // 检查描述是否合法
        String pattern = "^(?!.*\\|\\|)(?!.*#\\|#).*$";
        if (!description.matches(pattern)) {
            throw new IllegalArgumentException("权限描述不合法，不能包含||或#|#");
        }

        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null) {
            throw new IllegalArgumentException("权限不存在");
        }

        // 更新权限描述并持久化
        permission.setDescription(description);
        permission.setUpdateTime(LocalDateTime.now());
        permissionMapper.updateById(permission);
    }

    @Override
    @GlobalTransactional
    public void deletePermission(String permissionId) {
        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null) {
            throw new IllegalArgumentException("权限不存在");
        }

        permissionMapper.deleteById(permissionId);

        // 获取角色Name，清除缓存
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(Wrappers
                .<RolePermission>lambdaQuery()
                .eq(RolePermission::getPermissionId, permissionId)
        );

        rolePermissionMapper.delete(Wrappers
                .<RolePermission>lambdaUpdate()
                .eq(RolePermission::getPermissionId, permissionId)
        );

        if (rolePermissions != null && !rolePermissions.isEmpty()) {
            authRedisUtil.deleteRedisRolePermissionByIds(rolePermissions.stream().map(RolePermission::getRoleId).toList());
        }
    }

    @Override
    public List<PermissionVO> getUserPermissions(String userId) {
        if (userId == null || userId.isEmpty()) {
            userId = StpUtil.getLoginIdAsString();
        } else {
            // 权限鉴定
            StpUtil.checkPermission(PermissionUtil.generate(PermModule.ROLE_PERMISSION, PermAction.READ));
        }

        // 从数据库查询Permission
        List<Permission> permissions = permissionMapper.selectPermissionsByUserId(userId);
        return BeanUtil.copyToList(permissions, PermissionVO.class);
    }

    @Override
    public List<PermissionVO> getRolePermissions(String roleId) {
        // 从数据库查询角色权限
        List<Permission> permissions = permissionMapper.selectPermissionsByRoleId(roleId);

        // 转换为 PermissionVO 列表
        return BeanUtil.copyToList(permissions, PermissionVO.class);
    }

    @Override
    @GlobalTransactional
    public void updateRolePermission(String roleId, String permissionId) {
        String roleName = roleMapper.selectById(roleId).getRoleName();

        Permission permission = permissionMapper.selectOne(Wrappers
                .<Permission>lambdaQuery()
                .eq(Permission::getPermissionId, permissionId)
        );

        if (permission == null || permission.getPermissionId().isEmpty()) {
            throw new IllegalArgumentException("无此权限ID"); // 好像没啥用，但是加强一下，防小人
        }

        // 插入新的角色权限
        RolePermission rolePermission = RolePermission
                .builder()
                .roleId(roleId)
                .permissionId(permissionId)
                .build();
        rolePermissionMapper.insert(rolePermission);

        authRedisUtil.deleteRedisRolePermission(roleName);
    }

    @Override
    @GlobalTransactional
    public void updateRolePermissions(String roleId, List<String> permissionIds) {
        String roleName = roleMapper.selectById(roleId).getRoleName();

        permissionIds = permissionMapper.selectList(Wrappers
                .<Permission>lambdaQuery()
                .in(Permission::getPermissionId, permissionIds)
        ).stream().map(Permission::getPermissionId).toList();

        if (permissionIds.isEmpty()) {
            throw new IllegalArgumentException("无此权限ID"); // 好像没啥用，但是加强一下，防小人
        }

        // 删除现有角色权限
        rolePermissionMapper.delete(Wrappers
                .<RolePermission>lambdaQuery()
                .eq(RolePermission::getRoleId, roleId)
        );

        // 插入新的角色权限
        List<RolePermission> rolePermissions = permissionIds.stream().map(permissionId ->
                RolePermission.builder()
                        .roleId(roleId)
                        .permissionId(permissionId)
                        .build()
        ).toList();
        rolePermissionMapper.insert(rolePermissions, 50);

        authRedisUtil.deleteRedisRolePermission(roleName);
    }

    @Override
    @GlobalTransactional
    public void deleteRolePermissions(RolePermissionDTO rolePermissionDTO) {
        String roleName = roleMapper.selectById(rolePermissionDTO.getRoleId()).getRoleName();

        // 删除角色的所有权限
        rolePermissionMapper.delete(Wrappers
                .<RolePermission>lambdaQuery()
                .eq(RolePermission::getRoleId, rolePermissionDTO.getRoleId())
                .in(RolePermission::getPermissionId, rolePermissionDTO.getPermissionIds())
        );

        authRedisUtil.deleteRedisRolePermission(roleName);
    }

}
