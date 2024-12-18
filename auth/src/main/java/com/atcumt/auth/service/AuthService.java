package com.atcumt.auth.service;

import com.atcumt.model.auth.dto.RegisterDTO;
import com.atcumt.model.auth.entity.AppleAuth;
import com.atcumt.model.auth.entity.QqAuth;
import com.atcumt.model.auth.entity.UserAuth;
import com.atcumt.model.auth.vo.AuthenticationVO;
import com.atcumt.model.auth.vo.LinkedAccountVO;
import com.atcumt.model.auth.vo.SensitiveRecordVO;
import com.atcumt.model.auth.vo.TokenVO;
import com.atcumt.model.common.PageQueryVO;
import com.atcumt.model.common.TypePageQueryDTO;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface AuthService extends IService<UserAuth> {
    TokenVO registerBySchool(String schoolToken) throws Exception;

    TokenVO loginBySchool(String schoolToken) throws Exception;

    TokenVO refreshToken(String refreshToken);

    void logout(String device);

    void bindUsername(String userId, String username, String password);

    TokenVO loginByUsernamePassword(String username, String password) throws Exception;

    void bindEmail(String userId, String email, String verificationCode) throws Exception;

    void SendVerifyCode(String email, String captchaId, String captchaCode) throws Exception;

    TokenVO loginByEmailVerificationCode(String email, String verificationCode);

    void updateUsername(String unifiedToken, String userId, String username);

    void updatePassword(String unifiedToken, String userId, String password);

    void updateEmail(String unifiedToken, String userId, String verificationCode, String email);

    void sendCaptcha(HttpServletResponse response) throws Exception;

    AuthenticationVO authenticationByUnifiedAuth(String cookie);

    TokenVO register(RegisterDTO registerDTO);

    TokenVO loginByUnifiedAuth(String cookie);

    TokenVO loginByQQ(String qqAuthorizationCode);

    void changeEmail(String verificationCode, String email);

    void changeUsername(String username);

    void changePassword(String oldPassword, String newPassword) throws Exception;

    void resetPassword(String cookie, String password);

    void deleteAccount(String password) throws Exception;

    PageQueryVO<SensitiveRecordVO> getSensitiveRecord(TypePageQueryDTO typePageQueryDTO);

    void unBindQQ();

    QqAuth bindQQ(String qqAuthorizationCode);

    AppleAuth bindApple(String appleAuthorizationCode);

    void unBindApple();

    TokenVO loginByApple(String appleAuthorizationCode);

    LinkedAccountVO getLinkedAccount();

    String getUsername();

    List<String> getLoginDevices();
}
