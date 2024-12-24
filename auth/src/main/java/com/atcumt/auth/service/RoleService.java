package com.atcumt.auth.service;

import com.atcumt.model.auth.dto.RoleDTO;
import com.atcumt.model.auth.dto.UserRoleDTO;
import com.atcumt.model.auth.entity.Role;
import com.atcumt.model.auth.vo.RoleVO;
import com.atcumt.model.common.dto.PageQueryDTO;
import com.atcumt.model.common.vo.PageQueryVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface RoleService extends IService<Role> {
    List<RoleVO> getUserRole(String userId);

    void updateUserRole(String userId, String roleId);

    void updateUserRoles(UserRoleDTO userRoleDTO);

    void deleteUserRole(UserRoleDTO userRoleDTO);

    PageQueryVO<RoleVO> getAllRole(PageQueryDTO pageQueryDTO);

    RoleVO createRole(RoleDTO roleDTO);

    void updateRoleDescription(String roleId, String description);

    void deleteRole(String roleId);
}
