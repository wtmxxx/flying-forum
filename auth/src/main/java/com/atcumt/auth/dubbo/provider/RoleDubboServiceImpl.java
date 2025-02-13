package com.atcumt.auth.dubbo.provider;

import cn.hutool.core.bean.BeanUtil;
import com.atcumt.auth.mapper.RoleMapper;
import com.atcumt.common.api.auth.RoleDubboService;
import com.atcumt.model.auth.entity.Role;
import com.atcumt.model.auth.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
@RequiredArgsConstructor
public class RoleDubboServiceImpl implements RoleDubboService {
    private final RoleMapper roleMapper;

    @Override
    public List<RoleVO> getUserRole(String userId) {
        // 从数据库查询Role
        List<Role> roles = roleMapper.selectRolesByUserId(userId);
        return BeanUtil.copyToList(roles, RoleVO.class);
    }
}
