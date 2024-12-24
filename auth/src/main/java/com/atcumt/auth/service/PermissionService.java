package com.atcumt.auth.service;

import com.atcumt.model.auth.dto.PermissionDTO;
import com.atcumt.model.auth.dto.RolePermissionDTO;
import com.atcumt.model.auth.entity.Permission;
import com.atcumt.model.auth.vo.PermissionVO;
import com.atcumt.model.auth.vo.SortedPermissionVO;
import com.atcumt.model.common.dto.PageQueryDTO;
import com.atcumt.model.common.vo.PageQueryVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PermissionService extends IService<Permission> {
    PageQueryVO<PermissionVO> getAllPermissions(PageQueryDTO pageQueryDTO);

    PageQueryVO<SortedPermissionVO> getAllSortedPermissions(PageQueryDTO pageQueryDTO);

    PermissionVO createPermission(PermissionDTO permissionDTO);

    void updatePermissionDescription(String permissionId, String description);

    void deletePermission(String permissionId);

    List<PermissionVO> getUserPermissions(String userId);

    List<PermissionVO> getRolePermissions(String roleId);

    void updateRolePermission(String roleId, String permissionId);

    void updateRolePermissions(String roleId, List<String> permissionIds);

    void deleteRolePermissions(RolePermissionDTO rolePermissionDTO);
}
