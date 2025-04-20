package com.atcumt.auth.service;

import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.common.exception.BadRequestException;
import com.atcumt.common.exception.UnauthorizedException;
import com.atcumt.model.auth.dto.RegisterDTO;
import com.atcumt.model.auth.entity.AppleAuth;
import com.atcumt.model.auth.entity.QqAuth;
import com.atcumt.model.auth.entity.UserAuth;
import com.atcumt.model.auth.vo.*;
import com.atcumt.model.common.dto.TypePageQueryDTO;
import com.atcumt.model.common.vo.PageQueryVO;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface AuthService extends IService<UserAuth> {
    TokenVO registerBySchool(String schoolToken) throws Exception;

    TokenVO loginBySchool(String schoolToken) throws Exception;

    TokenVO refreshToken(String refreshToken) throws UnauthorizedException;

    void logout(String device);

    void bindUsername(String userId, String username, String password);

    TokenVO loginByUsernamePassword(String username, String password) throws Exception;

    void sendVerifyCodeWithCaptcha(String email, String captchaId, String captchaCode) throws Exception;

    void bindEmail(String userId, String email, String verificationCode) throws Exception;

    void sendVerifyCode(String email) throws Exception;

    TokenVO loginByEmailVerificationCode(String email, String verificationCode) throws AuthorizationException;

    void updateUsername(String unifiedToken, String userId, String username) throws BadRequestException, UnauthorizedException;

    void updatePassword(String unifiedToken, String userId, String password) throws UnauthorizedException;

    void updateEmail(String unifiedToken, String userId, String verificationCode, String email) throws UnauthorizedException;

    void sendCaptcha(HttpServletResponse response) throws Exception;

    AuthenticationVO authenticationByUnifiedAuth(String cookie) throws AuthorizationException, UnauthorizedException;

    String registerPreCheck(RegisterDTO registerDTO);

    TokenVO register(RegisterDTO registerDTO, String sid) throws Exception;

    TokenVO loginByUnifiedAuth(String cookie) throws AuthorizationException, UnauthorizedException;

    TokenVO loginByQQ(String qqAuthorizationCode) throws AuthorizationException;

    void changeEmail(String verificationCode, String email);

    void changeUsername(String username) throws BadRequestException;

    void changePassword(String oldPassword, String newPassword) throws Exception;

    void resetPassword(String cookie, String password) throws AuthorizationException, UnauthorizedException;

    void deleteAccount(String password) throws Exception;

    PageQueryVO<SensitiveRecordVO> getSensitiveRecord(TypePageQueryDTO typePageQueryDTO);

    QqAuth bindQQ(String qqAuthorizationCode, String userId) throws AuthorizationException;

    void unBindQQ();

    QqAuth bindQQ(String qqAuthorizationCode) throws AuthorizationException;

    AppleAuth bindApple(String appleAuthorizationCode) throws Exception;

    AppleAuth bindApple(String appleAuthorizationCode, String userId) throws Exception;

    void unBindApple();

    TokenVO loginByApple(String appleAuthorizationCode) throws Exception;

    LinkedAccountVO getLinkedAccount();

    String getUsername();

    List<DeviceVO> getLoginDevices();
}
