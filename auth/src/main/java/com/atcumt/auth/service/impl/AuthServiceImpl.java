package com.atcumt.auth.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.atcumt.auth.api.client.SchoolClient;
import com.atcumt.auth.mapper.AuthMapper;
import com.atcumt.auth.service.AuthService;
import com.atcumt.common.exception.UnauthorizedException;
import com.atcumt.common.utils.JwtTool;
import com.atcumt.model.auth.entity.UserAuth;
import com.atcumt.model.common.AuthMessage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends ServiceImpl<AuthMapper, UserAuth> implements AuthService {
    private final AuthMapper authMapper;
    private final JwtTool jwtTool;
    private final SchoolClient schoolClient;

    @Override
    @GlobalTransactional
    public String registerBySchool(String schoolToken) throws Exception {
        // 请求学校服务器获取profile
        String profile = schoolClient.getProfile(schoolToken);
        String schoolResults = JSONUtil.parseObj(profile).get("entities", String.class);
        if (!JSONUtil.parseObj(schoolResults).get("ecode", String.class).equals("SUCCEED")) {
            throw new UnauthorizedException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());
        }

        // 获取 results 下的 entities 数组
        JSONArray entities = JSONUtil.parseObj(profile).getByPath("results.entities", JSONArray.class);

        // 提取 account 值
        String studentId = entities.getJSONObject(0).getStr("account");

        UserAuth userAuth = authMapper.selectById(studentId);

        // 查看学号是否已注册
        if (userAuth != null) throw new Exception(AuthMessage.STUDENT_ID_ALREADY_REGISTERED.getMessage());
        else userAuth = UserAuth
                .builder()
                .studentId(studentId)
                .build();

        authMapper.insert(userAuth);

        // 创建JWT Token
        return jwtTool.createToken(userAuth.getUserId());
    }
}
