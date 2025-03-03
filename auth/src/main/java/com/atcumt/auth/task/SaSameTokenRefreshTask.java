package com.atcumt.auth.task;

import cn.dev33.satoken.same.SaSameUtil;
import com.atcumt.common.utils.RedisLockUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Same-Token，定时刷新
 */
@Component
@Slf4j
public class SaSameTokenRefreshTask {
    private final RedisLockUtil redisLockUtil;
    private static final String LOCK_KEY = "Authorization:var:same-token-refresh-lock";
    @Autowired
    SaSameTokenRefreshTask(@Qualifier("saRedisTemplate") RedisTemplate<String, String> saRedisTemplate) {
        this.redisLockUtil = RedisLockUtil.create(saRedisTemplate, 5, TimeUnit.MINUTES);
    }

    // 每隔 10 分钟刷新一次 Same-Token
    @Scheduled(cron = "0 0/10 * * * ?")
    public void refreshToken() {
        if (redisLockUtil.tryLock(LOCK_KEY)) {
            SaSameUtil.refreshToken();
        }
    }
}