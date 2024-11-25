package com.atcumt.common.utils;

public class UserContext {
    private static final ThreadLocal<String> threadLocal = new ThreadLocal<>();

    /**
     * 获取当前登录用户信息
     *
     * @return 用户id
     */
    public static String getUserId() {
        return threadLocal.get();
    }

    /**
     * 保存当前登录用户信息到ThreadLocal
     *
     * @param userId 用户id
     */
    public static void setUserId(String userId) {
        threadLocal.set(userId);
    }

    /**
     * 移除当前登录用户信息
     */
    public static void removeUserId() {
        threadLocal.remove();
    }
}
