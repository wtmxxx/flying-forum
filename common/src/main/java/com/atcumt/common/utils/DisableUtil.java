package com.atcumt.common.utils;

import cn.dev33.satoken.stp.StpUtil;
import com.atcumt.common.enums.DisableEnum;
import com.atcumt.common.enums.PermAction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisableUtil {

    /**
     * 鉴定权限并执行服务的禁用操作
     *
     * @param userId      用户ID
     * @param serviceName 服务名称
     * @param duration    封禁时长 (秒)
     */
    public static void disableService(String userId, String serviceName, Long duration) {
        // 获取对应服务的 DisableEnum
        DisableEnum service = DisableEnum.fromService(serviceName);

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(service.getModule(), service.getDisableAction()));

        // 如果是 ACCOUNT
        if (service == DisableEnum.ACCOUNT) {
            // 先踢下线
            StpUtil.kickout(userId);
        }

        // 封禁服务
        StpUtil.disable(userId, service.getModule().getValue(), duration);
    }

    /**
     * 鉴定权限并执行服务的解封操作
     *
     * @param userId      用户ID
     * @param serviceName 服务名称
     */
    public static void untieDisableService(String userId, String serviceName) {
        // 获取对应服务的 DisableEnum
        DisableEnum service = DisableEnum.fromService(serviceName);

        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(service.getModule(), service.getUntieDisableAction()));

        // 解封服务
        StpUtil.untieDisable(userId, service.getModule().getValue());
    }

    /**
     * 获取用户所有服务的封禁状态及剩余封禁时间
     *
     * @param userId      用户ID
     * @param serviceName 服务名称
     * @return 用户在所有服务中的封禁时长 Map（服务名称 -> 封禁时长）
     */
    public static Map<String, Long> getServiceDisableTime(String userId, String serviceName) {

        Map<String, Long> disableTimes = new HashMap<>();

        // 权限鉴定（有封禁或解封权限者可以查看服务被封禁时长）
        if (!StpUtil.hasPermissionOr(
                PermissionUtil.generate(
                        // service.disable
                        DisableEnum.fromService(serviceName).getModule(),
                        PermAction.DISABLE
                ),
                PermissionUtil.generate(
                        // service.untie_disable
                        DisableEnum.fromService(serviceName).getModule(),
                        PermAction.UNTIE_DISABLE
                )
        )) {
            // -3 代表无权查看
            disableTimes.put(serviceName, -3L);
        } else {
            // 获取服务的封禁剩余时间
            long disableTime;
            if (serviceName.equals(DisableEnum.ACCOUNT.getModule().getValue())) {
                disableTime = StpUtil.getDisableTime(userId);
            } else {
                disableTime = StpUtil.getDisableTime(userId, serviceName);
            }

            // 保存服务名称与剩余时间
            disableTimes.put(serviceName, disableTime);
        }

        return disableTimes;
    }

    /**
     * 获取用户所有服务的封禁状态及剩余封禁时间
     *
     * @param userId 用户ID
     * @return 用户在所有服务中的封禁时长 Map（服务名称 -> 封禁时长）
     */
    public static Map<String, Long> getAllServiceDisableTimes(String userId) {

        Map<String, Long> disableTimes = new HashMap<>();

        // 遍历所有服务
        for (String service : DisableEnum.getAllServices()) {
            // 权限鉴定（有封禁或解封权限者可以查看服务被封禁时长）
            if (!StpUtil.hasPermissionOr(
                    PermissionUtil.generate(
                            // service.disable
                            DisableEnum.fromService(service).getModule(),
                            PermAction.DISABLE
                    ),
                    PermissionUtil.generate(
                            // service.untie_disable
                            DisableEnum.fromService(service).getModule(),
                            PermAction.UNTIE_DISABLE
                    )
            )) {
                // -3 代表无权查看
                disableTimes.put(service, -3L);
                continue;
            }

            // 获取服务的封禁剩余时间
            long disableTime;
            if (service.equals(DisableEnum.ACCOUNT.getModule().getValue())) {
                disableTime = StpUtil.getDisableTime(userId);
            } else {
                disableTime = StpUtil.getDisableTime(userId, service);
            }

            // 保存服务名称与剩余时间
            disableTimes.put(service, disableTime);
        }

        return disableTimes;
    }

    /**
     * 获取所有服务的名称
     *
     * @return 所有服务名称的列表
     */
    public static List<String> getAllServices() {
        return DisableEnum.getAllServices();
    }
}
