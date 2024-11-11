package com.atcumt.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.jwt.JWT;
import com.atcumt.auth.api.client.SchoolClient;
import com.atcumt.auth.mapper.AuthMapper;
import com.atcumt.auth.service.AuthService;
import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.common.exception.UnauthorizedException;
import com.atcumt.model.auth.entity.UserAuth;
import com.atcumt.model.common.AuthMessage;
import com.atcumt.model.common.DeviceType;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends ServiceImpl<AuthMapper, UserAuth> implements AuthService {
    private final AuthMapper authMapper;
    private final SchoolClient schoolClient;

    @Override
    @GlobalTransactional
    public String registerBySchool(String schoolToken) {
        // 使用token获取学号
        String studentId = getStudentIdByToken(schoolToken);

        UserAuth userAuth = authMapper.selectOne(
                Wrappers.<UserAuth>lambdaQuery()
                        .eq(UserAuth::getStudentId, studentId)
        );

        // 查看学号是否已注册
        if (userAuth != null)
            throw new AuthorizationException(AuthMessage.STUDENT_ID_ALREADY_REGISTERED.getMessage(), 409);
        else userAuth = UserAuth
                .builder()
                .studentId(studentId)
                .build();

        authMapper.insert(userAuth);

        // 注册成功，进行登录，返回token
        StpUtil.login(userAuth.getUserId(), DeviceType.getDeviceType());
        return StpUtil.getTokenValueByLoginId(userAuth.getUserId(), DeviceType.getDeviceType());

        // 创建JWT Token
        // return jwtTool.createToken(userAuth.getUserId());
    }

    @Override
    @GlobalTransactional
    public String loginBySchool(String schoolToken) throws Exception {
        // 使用token获取学号
        String studentId = getStudentIdByToken(schoolToken);

        UserAuth userAuth = authMapper.selectOne(
                Wrappers.<UserAuth>lambdaQuery()
                        .eq(UserAuth::getStudentId, studentId)
        );

        // 查看学号是否已注册
        if (userAuth == null) throw new AuthorizationException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage(), 404);

        StpUtil.login(userAuth.getUserId(), DeviceType.getDeviceType());
        return StpUtil.getTokenValueByLoginId(userAuth.getUserId(), DeviceType.getDeviceType());
    }


    String getStudentIdByToken(String schoolToken) {
        // 请求学校服务器获取校园卡信息
        String schoolCard = schoolClient.getSchoolCard(schoolToken);
        // 解析JSON字符串
        JSONObject jsonObj = JSONUtil.parseObj(schoolCard);
        if (!jsonObj.get("success", Boolean.class)) {
            throw new UnauthorizedException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());
        }

        // 获取 results 下的 entities 数组
        // 获取data -> card数组中的第一个对象 -> sno字段
        String studentId = jsonObj.getByPath("data.card[0].sno", String.class);

        // 去掉"Bearer "前缀（如果存在）
        if (schoolToken.startsWith("Bearer ") || schoolToken.startsWith("bearer ")) {
            schoolToken = schoolToken.substring(7);
        }
        // JWT和学校双重验证
        JWT schoolJwt = JWT.of(schoolToken);
        if (studentId.equals(schoolJwt.getPayload("sno").toString())) {
            return studentId;
        } else {
            throw new UnauthorizedException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());
        }
    }
}
