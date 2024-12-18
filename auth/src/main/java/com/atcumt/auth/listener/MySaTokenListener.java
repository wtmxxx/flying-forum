package com.atcumt.auth.listener;

import cn.dev33.satoken.listener.SaTokenListenerForSimple;
import cn.dev33.satoken.stp.SaLoginModel;
import com.atcumt.auth.utils.IpUtil;
import com.atcumt.model.auth.entity.SensitiveRecord;
import com.atcumt.model.auth.enums.SensitiveRecordType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class MySaTokenListener extends SaTokenListenerForSimple {
    private final RocketMQTemplate rocketMQTemplate;
    private final IpUtil ipUtil;

    /*
     * SaTokenListenerForSimple 对所有事件提供了空实现，通过继承此类，你只需重写一部分方法即可实现一个可用的侦听器。
     */

    /**
     * 每次登录时触发
     */
    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginModel loginModel) {
        String userId = loginId.toString();
        String ip = ipUtil.getRemoteAddr();
        String region = ipUtil.getRegionByIp(ip);

        SensitiveRecord record = SensitiveRecord
                .builder()
                .userId(userId)
                .type(SensitiveRecordType.LOGIN.getType())
                .description(SensitiveRecordType.LOGIN.getDescription())
                .ip(ip)
                .region(region)
                .recordTime(LocalDateTime.now())
                .build();

        // 发送登录日志到消息队列
        asyncLogLogin(record);
    }

    public void asyncLogLogin(SensitiveRecord record) {
        rocketMQTemplate.asyncSend("auth:sensitiveRecord", record, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("登录日志记录成功");
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("登录日志记录失败", throwable);
            }
        });
    }
}
