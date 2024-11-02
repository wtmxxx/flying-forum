package com.atcumt.user.service.impl;

import com.atcumt.common.utils.JwtTool;
import com.atcumt.model.common.AuthMessage;
import com.atcumt.model.user.entity.Auth;
import com.atcumt.user.mapper.AuthMapper;
import com.atcumt.user.service.AuthService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends ServiceImpl<AuthMapper, Auth> implements AuthService {
    private final AuthMapper authMapper;
    private final JwtTool jwtTool;

    @Override
    @GlobalTransactional
    public String registerByStuId(String studentId, String unifiedPassword) throws Exception {
        Auth auth = authMapper.selectById(studentId);

        // 查看学号是否已注册
        if (auth != null) throw new Exception(AuthMessage.STUDENT_ID_ALREADY_REGISTERED.getMessage());
        else auth = Auth
                .builder()
                .studentId(studentId)
                .build();

        // TODO 统一身份认证登录
        Boolean login = true;

        if (login) {
            authMapper.insert(auth);
        } else {
            throw new Exception(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());
        }

        // 创建JWT Token
        return jwtTool.createToken(auth.getId());
    }
}
