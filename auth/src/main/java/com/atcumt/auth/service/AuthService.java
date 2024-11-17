package com.atcumt.auth.service;

import com.atcumt.model.auth.entity.UserAuth;
import com.atcumt.model.auth.vo.TokenVO;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService extends IService<UserAuth> {
    TokenVO registerBySchool(String schoolToken) throws Exception;

    TokenVO loginBySchool(String schoolToken) throws Exception;

    TokenVO refreshToken(String refreshToken);

    void logout();

    void bindUsername(String userId, String username, String password);

    TokenVO loginByUsernamePassword(String username, String password) throws Exception;

    void bindEmail(String userId, String email, String verificationCode) throws Exception;

    void SendVerifyCode(String email, String captchaId, String captchaCode) throws Exception;

    TokenVO loginByEmailVerificationCode(String email, String verificationCode);

    void updateUsername(String unifiedToken, String userId, String username);

    void updatePassword(String unifiedToken, String userId, String password);

    void updateEmail(String unifiedToken, String userId, String verificationCode, String email);

    void sendCaptcha(HttpServletResponse response) throws Exception;
}
