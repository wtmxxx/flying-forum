package com.atcumt.auth.dubbo.provider;

import cn.hutool.core.bean.BeanUtil;
import com.atcumt.auth.mapper.PermissionMapper;
import com.atcumt.common.api.auth.PermissionDubboService;
import com.atcumt.model.auth.entity.Permission;
import com.atcumt.model.auth.vo.PermissionVO;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
@RequiredArgsConstructor
public class PermissionDubboServiceImpl implements PermissionDubboService {
    private final PermissionMapper permissionMapper;

    @Override
    public List<PermissionVO> getRolePermissions(String roleId) {
        // 从数据库查询Permission
        List<Permission> permissions = permissionMapper.selectPermissionsByRoleId(roleId);
        return BeanUtil.copyToList(permissions, PermissionVO.class);
    }
}
