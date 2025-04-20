package com.atcumt.oss.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.RandomUtil;
import com.atcumt.common.utils.FileConvertUtil;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.oss.dto.FileInfoDTO;
import com.atcumt.model.oss.entity.FileInfo;
import com.atcumt.model.oss.entity.FileUser;
import com.atcumt.model.oss.vo.FileInfoVO;
import com.atcumt.oss.service.FileService;
import com.atcumt.oss.utils.FileCheckUtil;
import com.atcumt.oss.utils.ImageUtil;
import io.minio.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {
    private final MinioClient minioClient;
    private final FileCheckUtil fileCheckUtil;
    private final FileConvertUtil fileConvertUtil;
    private final MongoTemplate mongoTemplate;
    private final RedisTemplate<String, byte[]> redisTemplate;

    @Value("${minio.url}")
    private String url;

    @Value("${minio.bucketName}")
    private String bucketName;

    public static String generateSHA1Hash(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        return generateHash(inputStream, "SHA-1");
    }

    public static String generateSHA3256Hash(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        return generateHash(inputStream, "SHA3-256");
    }

    public static String generateHash(InputStream inputStream, String algorithm) throws IOException, NoSuchAlgorithmException {
        // 创建 MessageDigest 对象，指定 SHA-1 算法
        MessageDigest digest = MessageDigest.getInstance(algorithm);

        // 使用 BufferedInputStream 来提高读取性能
        try (BufferedInputStream bis = new BufferedInputStream(inputStream)) {
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

    @Override
    public FileInfoVO uploadAvatar(MultipartFile file) throws Exception {
        // 审查文件合法性
        fileCheckUtil.reviewAvatar(file);

        // 处理头像
        InputStream inputStream = ImageUtil.normalizeAvatar(file.getInputStream());

        // 获取图片大小
        long imageSize = ImageUtil.getImageSize(inputStream);

        // 获取图片文件名
        String filename = FileUtil.mainName(file.getOriginalFilename()) + ".jpg";

        return uploadFile(inputStream, filename, MimeTypeUtils.IMAGE_JPEG_VALUE, imageSize);
    }

    public FileInfoVO uploadFile(MultipartFile file) throws Exception {
        return uploadFile(file.getInputStream(), file.getOriginalFilename(), file.getContentType(), file.getSize());
    }

    public FileInfoVO uploadFile(InputStream inputStream, String originalFilename, String contentType, long fileSize) throws Exception {
        // 上传文件
        String filename = originalFilename;
        String extension = Optional.ofNullable(filename)
                .filter(f -> f.contains(".")) // 过滤掉没有扩展名的文件名
                .map(FileNameUtil::extName) // 提取扩展名
                .orElse(null); // 如果没有扩展名，返回空字符串

        String fileHash = generateSHA3256Hash(ImageUtil.copyInputStream(inputStream));

        if (extension == null) {
            filename = fileHash;
        } else {
            filename = fileHash + "." + extension;
        }

        FileInfo fileInfo = mongoTemplate.findOne(
                Query.query(Criteria
                        .where("filename").is(filename)
                        .and("bucket").is(bucketName)
                ),
                FileInfo.class
        );

        FileUser fileUser = FileUser.builder()
                .userId(UserContext.getUserId())
                .originalFilename(originalFilename)
                .uploadTime(LocalDateTime.now())
                .build();

        if (fileInfo == null) {
            PutObjectArgs putObjectArgs = PutObjectArgs
                    .builder()
                    .bucket(bucketName)
                    .object(filename)
                    .contentType(contentType)
                    .stream(inputStream, fileSize, -1)
                    .build();
            minioClient.putObject(putObjectArgs);

            fileInfo = FileInfo.builder()
                    .filename(filename)
                    .bucket(bucketName)
                    .version(1)
                    .size(fileSize)
                    .users(List.of(fileUser))
                    .build();

            updateFileInfo(fileInfo);
        } else {
            updateFileInfo(fileInfo, fileUser, 1);
        }

        // 创建文件信息对象

        return FileInfoVO
                .builder()
                .url(fileConvertUtil.convertToUrl(bucketName, filename))
                .bucket(bucketName)
                .fileName(filename)
                .originalName(originalFilename)
                .contentType(contentType)
                .size(fileSize)
                .uploadTime(LocalDateTime.now())
                .build();
    }

    @Async
    public void updateFileInfo(FileInfo fileInfo) {
        mongoTemplate.insert(fileInfo);
    }

    @Async
    public void updateFileInfo(FileInfo fileInfo, FileUser fileUser, Integer updateCount) {
        if (updateCount > 3) {
            return;
        }

        try {
            minioClient.statObject(StatObjectArgs
                    .builder()
                    .bucket(fileInfo.getBucket())
                    .object(fileInfo.getFilename())
                    .build());
        } catch (Exception e) {
            mongoTemplate.remove(new Query(Criteria
                    .where("filename").is(fileInfo.getFilename())
                    .and("bucket").is(bucketName)
            ), FileInfo.class);
            return;
        }

        mongoTemplate.updateFirst(
                Query.query(Criteria
                        .where("filename").is(fileInfo.getFilename())
                        .and("bucket").is(bucketName)
                ),
                new Update()
                        .inc("version", 1)
                        .push("users")
                        .slice(100)
                        .atPosition(Update.Position.FIRST)
                        .value(fileUser),
                FileInfo.class
        );

//        Map<String, FileUser> userMap = fileInfo.getUsers().stream()
//                .collect(Collectors.toMap(
//                        FileUser::getUserId,
//                        user -> user,
//                        (oldUser, newUser) -> oldUser.getUploadTime().isAfter(newUser.getUploadTime()) ? oldUser : newUser,
//                        HashMap::new
//                ));
//
//        userMap.put(fileUser.getUserId(), fileUser);
//
//        List<FileUser> users = userMap.values().stream()
//                .sorted(Comparator.comparing(FileUser::getUploadTime).reversed())
//                .limit(100).toList();
//        long modifiedCount = mongoTemplate.updateFirst(
//                Query.query(Criteria
//                        .where("filename").is(fileInfo.getFilename())
//                        .and("bucket").is(bucketName)
//                        .and("version").is(fileInfo.getVersion())
//                ),
//                new Update()
//                        .inc("version", 1)
//                        .set("users", users),
//                FileInfo.class
//        ).getModifiedCount();
//
//        if (modifiedCount == 0) {
//            FileInfo fileInfoV2 = mongoTemplate.findOne(
//                    Query.query(Criteria
//                            .where("filename").is(fileInfo.getFilename())
//                            .and("bucket").is(bucketName)
//                    ),
//                    FileInfo.class
//            );
//
//            if (fileInfoV2 == null) {
//                return;
//            }
//            updateFileInfo(fileInfoV2, fileUser, updateCount + 1);
//        }
    }

    @Override
    public void getFile(HttpServletResponse response, String bucket, String filename) {
        if (filename.contains("@")) {
            readFile(response, bucket, filename, false);
        } else {
            readOriginalFile(response, bucket, filename, false);
        }
    }

    @Override
    public void downloadFile(HttpServletResponse response, String bucket, String filename) {
        if (filename.contains("@")) {
            readFile(response, bucket, filename, true);
        } else {
            readOriginalFile(response, bucket, filename, true);
        }
    }

    public void readOriginalFile(HttpServletResponse response, String bucket, String filename, Boolean download) {
        try {
            String contentDisposition;
            if (download) {
                contentDisposition = "attachment; filename=Kxq_";
            } else {
                contentDisposition = "inline; filename=Kxq_";
            }
            contentDisposition += LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "." + FileNameUtil.extName(filename);
            String contentType = FileCheckUtil.getMIMEType(filename);

            response.addHeader("Content-Disposition", contentDisposition);
            response.setContentType(contentType);

            OutputStream outputStream = response.getOutputStream();

            InputStream fileStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)   // 存储桶名称
                            .object(filename)   // 对象名称（文件名）
                            .build()
            );

            byte[] buf = new byte[1024];
            int length;
            while ((length = fileStream.read(buf)) > 0) {
                outputStream.write(buf, 0, length);
            }
        } catch (Exception e) {
            log.error("资源获取失败", e);
            throw new IllegalArgumentException("资源获取失败");
        }
    }

    public void readFile(HttpServletResponse response, String bucket, String filename, Boolean download) {
        try {
            String params = List.of(filename.split("@")).getLast();
            String originalFilename = List.of(filename.split("@")).getFirst();

            int width = Integer.parseInt(List.of(params.split("[_.]")).get(0).replace("w", ""));
            int height = Integer.parseInt(List.of(params.split("[_.]")).get(1).replace("h", ""));

            String extName;
            String contentType;
            if (params.contains(".")) {
                extName = FileNameUtil.extName(params);
                contentType = FileCheckUtil.getMIMEType(params);
            } else if (originalFilename.contains(".")) {
                extName = FileNameUtil.extName(originalFilename);
                contentType = FileCheckUtil.getMIMEType(originalFilename);
            } else {
                extName = "";
                contentType = MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;
            }

            String contentDisposition;
            if (download) {
                contentDisposition = "attachment; filename=Kxq_thumb_";
            } else {
                contentDisposition = "inline; filename=Kxq_thumb_";
            }
            contentDisposition += LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "." + extName;

            response.addHeader("Content-Disposition", contentDisposition);
            response.setContentType(contentType);

            OutputStream outputStream = response.getOutputStream();

            byte[] cacheFile = redisTemplate.opsForValue().get("file:" + filename.replace("@", ":"));
            if (cacheFile != null && cacheFile.length > 0) {
                outputStream.write(cacheFile);
                return;
            }

            InputStream fileStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)   // 存储桶名称
                            .object(originalFilename)   // 对象名称（文件名）
                            .build()
            );

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            // 压缩图片
            BufferedImage compressImage = ImageUtil.compressImage(fileStream, width, height, extName);

            // 获取字节数组
            ImageIO.write(compressImage, extName, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            if (imageBytes.length <= 1024 * 1024) {
                redisTemplate.opsForValue().set("file:" + filename.replace("@", ":"), imageBytes, 888 + RandomUtil.randomInt(-8, 30), TimeUnit.HOURS);
            }

            outputStream.write(imageBytes);
        } catch (Exception e) {
            log.error("资源获取失败", e);
            throw new IllegalArgumentException("资源获取失败");
        }
    }

    @Override
    public void deleteFile(FileInfoDTO fileInfoDTO) throws Exception {
        // 判断时候为空
        if (fileInfoDTO == null || fileInfoDTO.getFileName() == null) {
            return;
        }

        // 删除文件
        Query fileQuery = Query.query(Criteria
                .where("filename").is(fileInfoDTO.getFileName())
                .and("bucket").is(fileInfoDTO.getBucket())
        );
        FileInfo fileInfo = mongoTemplate.findAndModify(
                fileQuery,
                new Update().inc("version", -1),
                FileInfo.class
        );
        if (fileInfo != null && fileInfo.getVersion() <= 1) {
            mongoTemplate.remove(fileQuery, FileInfo.class);

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(fileInfoDTO.getBucket())   // 存储桶名称
                            .object(fileInfoDTO.getFileName())   // 对象名称（文件名）
                            .build()
            );

            redisTemplate.delete("file:" + fileInfoDTO.getFileName() + ":*");
        }
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
