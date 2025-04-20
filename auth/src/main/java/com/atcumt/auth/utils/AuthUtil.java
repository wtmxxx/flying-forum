package com.atcumt.auth.utils;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AuthUtil {
    private final IpUtil ipUtil;

    public void login(Object id) {
        String ip = ipUtil.getRemoteAddr();
        String region = ipUtil.getRegionByIp(ip);
        StpUtil.login(id, new SaLoginParameter()
                .setDeviceType(DeviceUtil.getDeviceType())
                .setDeviceId(DeviceUtil.getDeviceId())
                .setTerminalExtraData(Map.of(
                        "deviceName", DeviceUtil.getDeviceName(),
                        "ip", Objects.requireNonNull(ip, "未知IP"),
                        "region", Objects.requireNonNull(region, "未知地区")
                ))
        );
    }
}
