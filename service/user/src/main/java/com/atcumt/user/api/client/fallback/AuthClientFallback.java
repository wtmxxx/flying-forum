package com.atcumt.user.api.client.fallback;

import com.atcumt.gateway.api.client.AuthClient;
import com.atcumt.model.auth.vo.PermissionVO;
import com.atcumt.model.auth.vo.RoleVO;
import com.atcumt.model.common.Result;
import com.atcumt.model.common.ResultCode;
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
                // 获取对话失败
                log.error("远程调用AuthClient#getRole方法出现异常，参数：{}", userId, cause);
                return Result.failure(ResultCode.BAD_REQUEST, cause.getMessage());
            }

            @Override
            public Result<List<PermissionVO>> getPermission(String roleId) {
                // 获取对话失败
                log.error("远程调用AuthClient#getPermission方法出现异常，参数：{}", roleId, cause);
                return Result.failure(ResultCode.BAD_REQUEST, cause.getMessage());
            }

        };
    }
}
