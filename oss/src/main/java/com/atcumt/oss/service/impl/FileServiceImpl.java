package com.atcumt.oss.service.impl;

import cn.hutool.core.io.file.FileNameUtil;
import com.atcumt.model.oss.dto.FileInfoDTO;
import com.atcumt.model.oss.vo.FileInfoVO;
import com.atcumt.oss.service.FileService;
import com.atcumt.oss.utils.FileCheckUtil;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {
    private final MinioClient minioClient;
    private final FileCheckUtil fileCheckUtil;

    @Value("${minio.url}")
    private String url;

    @Value("${minio.bucketName}")
    private String bucketName;

    public static String generateSHA1Hash(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        // 创建 MessageDigest 对象，指定 SHA-1 算法
        MessageDigest digest = MessageDigest.getInstance("SHA-1");

        // 使用 BufferedInputStream 来提高读取性能
        try (BufferedInputStream bis = new BufferedInputStream(file.getInputStream())) {
            byte[] byteArray = new byte[1024];
            int bytesRead;

            // 循环读取文件并将数据传递给 MessageDigest
            while ((bytesRead = bis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesRead);
            }
        }

        // 获取最终的 SHA-1 哈希值
        byte[] hashBytes = digest.digest();

        // 将字节数组转换为十六进制字符串
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));  // 以十六进制格式输出每个字节
        }

        return hexString.toString();  // 返回哈希值的字符串表示
    }

    // 上传文件并返回文件信息
    @Override
    public FileInfoVO uploadAllFile(MultipartFile file) throws Exception {
        // 审查文件合法性
        fileCheckUtil.reviewFile(file);

        return uploadFile(file);
    }

    @Override
    public List<FileInfoVO> uploadAllFiles(List<MultipartFile> files) throws Exception {
        List<FileInfoVO> fileInfoVOs = new ArrayList<>();
        for (MultipartFile file : files) {
            fileInfoVOs.add(uploadFile(file));
        }
        return fileInfoVOs;
    }

    @Override
    public FileInfoVO uploadMediaFile(MultipartFile file) throws Exception {
        // 审查文件合法性
        fileCheckUtil.reviewMediaFile(file);

        return uploadFile(file);
    }

    @Override
    public List<FileInfoVO> uploadMediaFiles(List<MultipartFile> files) throws Exception {
        List<FileInfoVO> fileInfoVOs = new ArrayList<>();
        for (MultipartFile file : files) {
            fileInfoVOs.add(uploadMediaFile(file));
        }
        return fileInfoVOs;
    }

    public FileInfoVO uploadFile(MultipartFile file) throws Exception {
        // 上传文件
        String fileName = file.getOriginalFilename();
        String extension = Optional.ofNullable(fileName)
                .filter(f -> f.contains(".")) // 过滤掉没有扩展名的文件名
                .map(FileNameUtil::extName) // 提取扩展名
                .orElse(null); // 如果没有扩展名，返回空字符串

        if (extension == null) {
            fileName = generateSHA1Hash(file);
        } else {
            fileName = generateSHA1Hash(file) + "." + extension;
        }

        String contentType = file.getContentType();
        long size = file.getSize();
        PutObjectArgs putObjectArgs = PutObjectArgs
                .builder()
                .bucket(bucketName)
                .object(fileName)
                .contentType(contentType)
                .stream(file.getInputStream(), size, -1)
                .build();
        minioClient.putObject(putObjectArgs);

        // 创建文件信息对象

        return FileInfoVO
                .builder()
                .url(url + "/" + bucketName + "/" + fileName)
                .bucket(bucketName)
                .fileName(fileName)
                .originalName(file.getOriginalFilename())
                .contentType(contentType)
                .size(size)
                .uploadTime(LocalDateTime.now())
                .build();
    }

    @Override
    public void deleteFile(FileInfoDTO fileInfoDTO) throws Exception {
        // 判断时候为空
        if (fileInfoDTO == null || fileInfoDTO.getFileName() == null) {
            return;
        }

        minioClient.removeObject(
                io.minio.RemoveObjectArgs.builder()
                        .bucket(fileInfoDTO.getBucket())   // 存储桶名称
                        .object(fileInfoDTO.getFileName())   // 对象名称（文件名）
                        .build()
        );

        log.info("成功删除文件, {}", fileInfoDTO.getFileName());
    }

    @Override
    public void deleteFiles(List<FileInfoDTO> fileInfoDTOs) {
        // 删除文件
        fileInfoDTOs.forEach(fileInfoDTO -> {
            try {
                deleteFile(fileInfoDTO);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
