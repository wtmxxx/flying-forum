package com.atcumt.common.utils;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import lombok.NonNull;

public class Argon2Util {
    private static final Argon2 argon2 = Argon2Factory.create(
            Argon2Factory.Argon2Types.ARGON2id, 32, 64
    );

    public static final int iterations = 10; // 迭代次数
    public static final int memory = 65536; // 内存使用量（单位：KB）
    public static final int parallelism = 2; // 并行度

    /**
     * 生成 Argon2 哈希
     * @param password 原始密码
     * @return Argon2 哈希值
     */
    public static String hash(@NonNull String password) {
        char[] passwordChars = password.toCharArray();
        try {
            // 生成哈希
//            Argon2Factory.create()
            return argon2.hash(iterations, memory, parallelism, passwordChars);
        } finally {
            // 清除敏感数据
            argon2.wipeArray(passwordChars);
        }
    }

    /**
     * 验证密码
     * @param hash 已存储的哈希
     * @param password 用户输入的密码
     * @return 是否匹配
     */
    public static boolean verify(String hash, String password) {
        return argon2.verify(hash, password.toCharArray());
    }
}
