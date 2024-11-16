package com.atcumt.auth.task;

import cn.dev33.satoken.same.SaSameUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Same-Token，定时刷新
 */
@Configuration
public class SaSameTokenRefreshTask {
    // 每隔 10 分钟刷新一次 Same-Token
    @Scheduled(cron = "0 0/10 * * * ? ")
    public void refreshToken() {
        SaSameUtil.refreshToken();
    }
}
