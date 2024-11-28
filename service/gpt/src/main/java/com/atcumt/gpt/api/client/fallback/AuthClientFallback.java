package com.atcumt.gpt.api.client.fallback;

import com.atcumt.gpt.api.client.AuthClient;
import com.atcumt.model.auth.vo.PermissionVO;
import com.atcumt.model.auth.vo.RoleVO;
import com.atcumt.model.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;

@Slf4j
public class AuthClientFallback implements FallbackFactory<AuthClient> {
    @Override
    public AuthClient create(Throwable cause) {
        return new AuthClient() {
            @Override
            public Result<List<RoleVO>> getRole(String userId) {
                log.error("远程调用AuthClient#getRole方法出现异常，参数：{}", userId, cause);
                throw new RuntimeException(cause);
            }

            @Override
            public Result<List<PermissionVO>> getPermission(String roleId) {
                log.error("远程调用AuthClient#getPermission方法出现异常，参数：{}", roleId, cause);
                throw new RuntimeException(cause);
            }

        };
    }
}
