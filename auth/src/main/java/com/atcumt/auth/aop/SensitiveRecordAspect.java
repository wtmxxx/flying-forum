package com.atcumt.auth.aop;

import cn.dev33.satoken.stp.StpUtil;
import com.atcumt.auth.utils.IpUtil;
import com.atcumt.model.auth.entity.SensitiveRecord;
import com.atcumt.model.auth.enums.SensitiveRecordType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Aspect
@EnableAspectJAutoProxy
@RequiredArgsConstructor
@Slf4j
public class SensitiveRecordAspect {
    private final RocketMQTemplate rocketMQTemplate;
    private final IpUtil ipUtil;

    @Pointcut("execution(* com.atcumt.auth.service.AuthService.changePassword(..))")
    public void changePasswordPointcut() {
    }

    @Pointcut("execution(* com.atcumt.auth.service.AuthService.changeUsername(..))")
    public void changeUsernamePointcut() {
    }

    @Pointcut("execution(* com.atcumt.auth.service.AuthService.changeEmail(..))")
    public void changeEmailPointcut() {
    }

    @Pointcut("execution(* com.atcumt.auth.service.AuthService.resetPassword(..))")
    public void resetPasswordPointcut() {
    }

    @Pointcut("execution(* com.atcumt.auth.service.AuthService.bindEmail(..))")
    public void bindEmailPointcut() {
    }

    @Pointcut("execution(* com.atcumt.auth.service.AuthService.bindQQ(..))")
    public void bindQQPointcut() {
    }

    @Pointcut("execution(* com.atcumt.auth.service.AuthService.bindApple(..))")
    public void bindApplePointcut() {
    }

    @Pointcut("execution(* com.atcumt.auth.service.AuthService.unBindQQ(..))")
    public void unBindQQPointcut() {
    }

    @Pointcut("execution(* com.atcumt.auth.service.AuthService.unBindApple(..))")
    public void unBindApplePointcut() {
    }

    @After("changePasswordPointcut()")
    public void logChangePassword() {
        asyncLogSensitiveRecord(SensitiveRecordType.CHANGE_PASSWORD);
    }

    @After("changeUsernamePointcut()")
    public void logChangeUsername() {
        asyncLogSensitiveRecord(SensitiveRecordType.CHANGE_USERNAME);
    }

    @After("changeEmailPointcut()")
    public void logChangeEmail() {
        asyncLogSensitiveRecord(SensitiveRecordType.CHANGE_EMAIL);
    }

    @After("resetPasswordPointcut()")
    public void logResetPassword() {
        asyncLogSensitiveRecord(SensitiveRecordType.RESET_PASSWORD);
    }

    @After("bindEmailPointcut() || bindQQPointcut() || bindApplePointcut()")
    public void logBindAccount() {
        asyncLogSensitiveRecord(SensitiveRecordType.BIND_ACCOUNT);
    }

    @After("unBindQQPointcut() || unBindApplePointcut()")
    public void logUnBindAccount() {
        asyncLogSensitiveRecord(SensitiveRecordType.UNBIND_ACCOUNT);
    }

    public void asyncLogSensitiveRecord(SensitiveRecordType type) {
        String userId = StpUtil.getLoginIdAsString();
        String ip = ipUtil.getRemoteAddr();
        String region = ipUtil.getRegionByIp(ip);

        SensitiveRecord record = SensitiveRecord
                .builder()
                .userId(userId)
                .type(type.getType())
                .description(type.getDescription())
                .ip(ip)
                .region(region)
                .recordTime(LocalDateTime.now())
                .build();

        rocketMQTemplate.asyncSend("auth:sensitiveRecord", record, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("{}日志记录成功", type.getDescription());
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("{}日志记录失败", type.getDescription(), throwable);
            }
        });
    }
}