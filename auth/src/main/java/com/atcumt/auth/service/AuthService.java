package com.atcumt.auth.service;

import com.atcumt.model.auth.entity.UserAuth;
import com.atcumt.model.auth.vo.TokenVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AuthService extends IService<UserAuth> {
    TokenVO registerBySchool(String schoolToken) throws Exception;

    TokenVO loginBySchool(String schoolToken) throws Exception;

    TokenVO refreshToken(String refreshToken);

    void logout();

    void bindUsername(String userId, String username, String password);

    TokenVO loginByUsernamePassword(String username, String password) throws Exception;

    void bindEmail(String userId, String email, String verificationCode) throws Exception;

    void SendVerifyCode(String email) throws Exception;

    TokenVO loginByEmailVerificationCode(String email, String verificationCode);
}
