package com.atcumt.user.api.client;

import com.atcumt.model.common.entity.Result;
import com.atcumt.model.oss.vo.FileInfoVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(value = "oss-service/api/file/v1")
public interface OssClient {
    @PostMapping(path = "/upload/avatar", consumes = "multipart/form-data")
    Result<FileInfoVO> uploadAvatar(MultipartFile file);
}
