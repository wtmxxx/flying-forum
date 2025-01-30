package com.atcumt.oss.utils;

import cn.hutool.core.io.file.FileNameUtil;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidParameterException;

@Component
public class FileCheckUtil {
    // 限制文件的最大大小 (以字节为单位)
    private static final long MAX_FILE_SIZE = 5368709120L; // 5GB
    private static final long MAX_AVATAR_SIZE = 10485760L; // 10MB
    // 定义不支持的扩展名常量
    private static final String[] UNSUPPORTED_EXTENSIONS = {
            "exe",  // 可执行文件
            "bat",  // Windows 批处理脚本
            "sh",   // Shell 脚本
            "dll",  // 动态链接库
            "com",  // 早期的可执行文件
            "cmd",  // Windows 命令文件
            "msi",  // Microsoft 安装包
            "scr",  // 屏幕保护程序文件
            "vbs",  // Visual Basic 脚本
            "wsf",  // Windows 脚本文件
            "pif",  // 程序信息文件
            "bin",  // 二进制可执行文件
            "elf",  // ELF 可执行文件（Linux）
            "run",  // Linux 可执行安装包
            "cgi",  // 通用网关接口脚本
            "pl",   // Perl 脚本
            "py",   // Python 脚本
            "app",  // Mac 应用程序包
            "command",  // Mac 可执行脚本
            "dmg",  // Mac 磁盘镜像文件
            "pkg",  // Mac 安装包
            "jar",  // Java 可执行文件
            "hta",  // HTML 应用程序文件
            "vbe",  // VBScript 编码文件
            "jse",  // JScript 编码文件
            "docm", // 带有宏的 Word 文件
            "xlsm", // 带有宏的 Excel 文件
            "pptm", // 带有宏的 PowerPoint 文件
            "vba",  // Visual Basic for Applications 脚本
            "ps1",  // PowerShell 脚本
            "php",  // PHP 脚本文件
            "asp"   // ASP 脚本文件
    };

    private static final Tika tika = new Tika();

    // 判断文件扩展名是否在不支持的列表中
    private static boolean isUnsupportedExtension(String extension) {
        for (String ext : UNSUPPORTED_EXTENSIONS) {
            if (ext.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMediaMIME(String mime) {
        if (mime == null || mime.isEmpty()) return false;
        return mime.startsWith("image/")
//                || mime.startsWith("video/")
//                || mime.startsWith("audio/")
                ;
    }

    private static boolean isAvatarMIME(String mime) {
        if (mime == null || mime.isEmpty()) return false;
        return mime.startsWith("image/png")
                || mime.startsWith("image/jpeg")
                || mime.startsWith("image/gif")
                ;
    }

    public static String getMIMEType(String extension) {
        return tika.detect(extension);
    }

    // 检查文件是否支持
    public static String validateFile(MultipartFile file) throws IOException {
        String fileExtension = FileNameUtil.extName(file.getOriginalFilename());

        // 1. 如果文件扩展名属于不支持的类型，抛出异常
        if (isUnsupportedExtension(fileExtension)) {
            throw new IllegalArgumentException("不支持的文件扩展名: " + fileExtension);
        }

        // 2. 获取 Tika 检测到的 MIME 类型
        String detectedMimeType = tika.detect(file.getInputStream());

        // 3. 比较文件的扩展名与 Tika 检测到的 MIME 类型
        String expectedMimeType = tika.detect(file.getOriginalFilename());
        // 仅比较是不是同一个前缀类型
        if (expectedMimeType != null && detectedMimeType != null) {
            if (!detectedMimeType.equals(expectedMimeType)) {
                if (isMediaMIME(detectedMimeType)) {
                    if (!detectedMimeType.split("/")[0].equals(expectedMimeType.split("/")[0])) {
                        throw new IllegalArgumentException("文件扩展名与实际 MIME 类型不匹配。扩展名: " + fileExtension + "，实际 MIME 类型: " + detectedMimeType);
                    }
                } else {
                    throw new IllegalArgumentException("文件扩展名与实际 MIME 类型不匹配。扩展名: " + fileExtension + "，实际 MIME 类型: " + detectedMimeType);
                }
            }
        } else {
            throw new IllegalArgumentException("文件扩展名与实际 MIME 类型不匹配。扩展名: " + fileExtension + "，实际 MIME 类型: " + (detectedMimeType == null ? "null" : detectedMimeType));
        }

        System.out.println("文件通过验证: " + file.getOriginalFilename());

        return detectedMimeType;
    }

    public void reviewFile(MultipartFile file) throws IOException {
        // 检查文件是否为空
        if (file.isEmpty()) {
            throw new InvalidParameterException("文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidParameterException("文件大小不能超过 5GB");
        }

        // 检查文件类型
        validateFile(file);

        // TODO 检查文件内容
        // 可以用MQ异步审查
    }

    public void reviewMediaFile(MultipartFile file) throws IOException {
        // 检查文件是否为空
        if (file.isEmpty()) {
            throw new InvalidParameterException("文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidParameterException("文件大小不能超过 5GB");
        }

        // 检查文件类型
        String mime = validateFile(file);

        if (!isMediaMIME(mime)) {
            throw new InvalidParameterException("文件类型错误");
        }

        // TODO 检查文件内容
        // 可以用MQ异步审查
    }

    public void reviewAvatar(MultipartFile file) throws IOException {
        // 检查文件是否为空
        if (file.isEmpty()) {
            throw new InvalidParameterException("文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new InvalidParameterException("文件大小不能超过 10MB");
        }

        // 检查文件类型
        String mime = validateFile(file);

        if (!isAvatarMIME(mime)) {
            throw new InvalidParameterException("文件类型错误");
        }
    }
}
