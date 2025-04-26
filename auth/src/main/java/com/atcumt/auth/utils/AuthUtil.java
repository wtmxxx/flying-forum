package com.atcumt.auth.utils;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthUtil {
    private final IpUtil ipUtil;

    public void login(Object id) {
        String ip = ipUtil.getRemoteAddr();
        String region = ipUtil.getRegionByIp(ip);

        if (ip == null) ip = "未知IP";
        if (region == null) region = "未知地区";

        StpUtil.login(id, new SaLoginParameter()
                .setDeviceType(DeviceUtil.getDeviceType())
                .setDeviceId(DeviceUtil.getDeviceId())
                .setTerminalExtraData(Map.of(
                        "deviceName", DeviceUtil.getDeviceName(),
                        "ip", ip,
                        "region", region
                ))
        );
    }
}
