package com.atcumt.common.api.auth;

import com.atcumt.model.auth.vo.RoleVO;

import java.util.List;

public interface RoleDubboService {
    List<RoleVO> getUserRole(String userId);
}
