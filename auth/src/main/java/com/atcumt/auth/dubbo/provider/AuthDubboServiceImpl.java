package com.atcumt.auth.dubbo.provider;

import com.atcumt.auth.mapper.AuthMapper;
import com.atcumt.common.api.auth.AuthDubboService;
import com.atcumt.model.auth.entity.UserAuth;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
@RequiredArgsConstructor
public class AuthDubboServiceImpl implements AuthDubboService {
    private final AuthMapper authMapper;

    @Override
    public String getUsername(String userId) {
        UserAuth userAuth = authMapper.selectOne(Wrappers
                .<UserAuth>lambdaQuery()
                .eq(UserAuth::getUserId, userId)
                .select(UserAuth::getUsername)
        );
        if (userAuth == null) {
            return "未知用户";
        }
        return userAuth.getUsername();
    }
}
