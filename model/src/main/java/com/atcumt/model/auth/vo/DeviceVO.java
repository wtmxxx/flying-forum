package com.atcumt.model.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceVO {
    private String deviceType; // 设备类型
    private String deviceName; // 设备名称
    private String region; // 登录地区
    private LocalDateTime lastLoginTime; // 上次登录时间
}
