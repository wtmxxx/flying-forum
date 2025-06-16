package com.atcumt.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.atcumt.auth.mapper.RoleMapper;
import com.atcumt.auth.mapper.RolePermissionMapper;
import com.atcumt.auth.mapper.UserRoleMapper;
import com.atcumt.auth.service.RoleService;
import com.atcumt.auth.utils.AuthRedisUtil;
import com.atcumt.common.enums.PermAction;
import com.atcumt.common.enums.PermModule;
import com.atcumt.common.utils.PermissionUtil;
import com.atcumt.model.auth.dto.RoleDTO;
import com.atcumt.model.auth.dto.UserRoleDTO;
import com.atcumt.model.auth.entity.Role;
import com.atcumt.model.auth.entity.RolePermission;
import com.atcumt.model.auth.entity.UserRole;
import com.atcumt.model.auth.vo.RoleVO;
import com.atcumt.model.common.dto.PageQueryDTO;
import com.atcumt.model.common.vo.PageQueryVO;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final AuthRedisUtil authRedisUtil;

    public RoleServiceImpl(
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            RolePermissionMapper rolePermissionMapper,
            AuthRedisUtil authRedisUtil
    ) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.authRedisUtil = authRedisUtil;
    }

    @Override
    public List<RoleVO> getUserRole(String userId) {
        if (userId == null || userId.isEmpty()) {
            userId = StpUtil.getLoginIdAsString();
        } else {
            // 权限鉴定
            StpUtil.checkPermission(PermissionUtil.generate(PermModule.USER_ROLE, PermAction.READ));
        }

        // 从数据库查询Role
        List<Role> roles = roleMapper.selectRolesByUserId(userId);
        return BeanUtil.copyToList(roles, RoleVO.class);
    }

    @Override
//    @GlobalTransactional
    @Transactional
    public void updateUserRole(String userId, String roleId) {
        if (userId == null || userId.isEmpty()) {
            userId = StpUtil.getLoginIdAsString();
        }

        // 查询 role 表中有效的 role_id 列表
        List<String> validRoleIds = roleMapper.getValidRoleIds(Collections.singletonList(roleId));

        // 如果没有合法的 roleId，抛出异常（你个老六）
        if (validRoleIds.isEmpty()) {
            throw new IllegalArgumentException("无效的角色ID，无法插入用户角色数据！");
        }

        String tmpUserId = userId;

        UserRole userRole = UserRole
                .builder()
                .userId(tmpUserId)
                .roleId(validRoleIds.getFirst())
                .build();

        userRoleMapper.insert(userRole);

        authRedisUtil.deleteRedisUserRole(userId);
    }

    @Override
//    @GlobalTransactional
    @Transactional
    public void updateUserRoles(UserRoleDTO userRoleDTO) {
        String userId = userRoleDTO.getUserId();
        if (userId == null || userId.isEmpty()) {
            userId = StpUtil.getLoginIdAsString();
        }

        // 查询 role 表中有效的 role_id 列表
        List<String> validRoleIds = roleMapper.getValidRoleIds(userRoleDTO.getRoleIds());

        // 如果没有合法的 roleId，抛出异常（你个老六）
        if (validRoleIds.isEmpty()) {
            throw new IllegalArgumentException("无效的角色ID，无法插入用户角色数据！");
        }

        userRoleMapper.delete(Wrappers
                .<UserRole>lambdaQuery()
                .eq(UserRole::getUserId, userId)
        );

        String tmpUserId = userId;

        List<UserRole> userRoles = validRoleIds.stream().map(roleId ->
                UserRole
                        .builder()
                        .userId(tmpUserId)
                        .roleId(roleId)
                        .build()
        ).toList();

        userRoleMapper.insert(userRoles, 50);

        authRedisUtil.deleteRedisUserRole(userId);
    }

    @Override
//    @GlobalTransactional
    @Transactional
    public void deleteUserRole(UserRoleDTO userRoleDTO) {
        String userId = userRoleDTO.getUserId();
        if (userId == null || userId.isEmpty()) {
            userId = StpUtil.getLoginIdAsString();
        }

        // 查询 role 表中有效的 role_id 列表
        List<String> validRoleIds = roleMapper.getValidRoleIds(userRoleDTO.getRoleIds());

        // 如果没有合法的 roleId，抛出异常（你个老六）
        if (validRoleIds.isEmpty()) {
            throw new IllegalArgumentException("无效的角色ID，无法插入用户角色数据！");
        }

        userRoleMapper.delete(Wrappers
                .<UserRole>lambdaQuery()
                .eq(UserRole::getUserId, userId)
                .in(UserRole::getRoleId, userRoleDTO.getRoleIds())
        );

        authRedisUtil.deleteRedisUserRole(userId);
    }

    @Override
    public RoleVO createRole(RoleDTO roleDTO) {
        Role role = roleMapper.selectOne(Wrappers
                .<Role>lambdaQuery()
                .eq(Role::getRoleName, roleDTO.getRoleName())
        );

        if (role != null) {
            throw new IllegalArgumentException("角色已存在");
        }

        role = Role
                .builder()
                .roleName(roleDTO.getRoleName())
                .description(roleDTO.getDescription())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        roleMapper.insert(role);

        return BeanUtil.toBean(role, RoleVO.class);
    }

    @Override
    public void updateRoleDescription(String roleId, String description) {
        roleMapper.updateById(Role.builder()
                .roleId(roleId)
                .description(description)
                .build()
        );
    }

    @Override
//    @GlobalTransactional
    @Transactional
    public void deleteRole(String roleId) {
        // 获取用户ID，清除缓存
        List<UserRole> userRole = userRoleMapper.selectList(Wrappers
                .<UserRole>lambdaQuery()
                .in(UserRole::getRoleId, roleId)
        );
        // 获取角色Name，清除缓存
        Role role = roleMapper.selectOne(Wrappers
                .<Role>lambdaQuery()
                .eq(Role::getRoleId, roleId)
                .select(Role::getRoleName)
        );
        if (role == null) throw new IllegalArgumentException("角色不存在");
        String roleName = role.getRoleName();

        userRoleMapper.delete(Wrappers
                .<UserRole>lambdaUpdate()
                .in(UserRole::getRoleId, roleId)
        );

        rolePermissionMapper.delete(Wrappers
                .<RolePermission>lambdaUpdate()
                .in(RolePermission::getRoleId, roleId)
        );

        roleMapper.deleteById(roleId);

        authRedisUtil.deleteRedisUserRole(userRole.stream().map(UserRole::getUserId).toList());
        authRedisUtil.deleteRedisRolePermission(roleName);
    }

    @Override
    public PageQueryVO<RoleVO> getAllRole(PageQueryDTO pageQueryDTO) {
        Page<Role> rolePage = Page.of(pageQueryDTO.getPage(), pageQueryDTO.getSize());
        rolePage.addOrder(OrderItem.desc("role_name"), OrderItem.asc("update_time"));

        rolePage = roleMapper.selectPage(rolePage, Wrappers.lambdaQuery());

        return PageQueryVO
                .<RoleVO>staticBuilder()
                .totalRecords(rolePage.getTotal())
                .totalPages(rolePage.getPages())
                .page(rolePage.getCurrent())
                .size(rolePage.getSize())
                .data(BeanUtil.copyToList(rolePage.getRecords(), RoleVO.class))
                .build();
    }
}
