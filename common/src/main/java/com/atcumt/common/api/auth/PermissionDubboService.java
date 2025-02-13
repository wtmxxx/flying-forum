package com.atcumt.common.api.auth;

import com.atcumt.model.auth.vo.PermissionVO;

import java.util.List;

public interface PermissionDubboService {
    List<PermissionVO> getRolePermissions(String userId);
}
