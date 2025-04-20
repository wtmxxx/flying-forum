package com.atcumt.user.api.client;

import com.atcumt.model.common.entity.Result;
import com.atcumt.model.oss.vo.FileInfoVO;
import com.atcumt.user.api.client.fallback.OssClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(value = "oss-service/api/file/v1", fallbackFactory = OssClientFallback.class)
public interface OssClient {
    @PostMapping(path = "/upload/avatar", consumes = "multipart/form-data")
    Result<FileInfoVO> uploadAvatar(@RequestPart("file") MultipartFile file);
}
