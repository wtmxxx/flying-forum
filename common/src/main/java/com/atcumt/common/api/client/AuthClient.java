package com.atcumt.common.api.client;

import com.atcumt.model.auth.vo.PermissionVO;
import com.atcumt.model.auth.vo.RoleVO;
import com.atcumt.model.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(value = "auth-service/api/admin")
public interface AuthClient {
    @GetMapping("/role/v1/user")
    Result<List<RoleVO>> getRole(String userId);

    @GetMapping("/permission/v1/role")
    Result<List<PermissionVO>> getPermission(String roleId);
}
