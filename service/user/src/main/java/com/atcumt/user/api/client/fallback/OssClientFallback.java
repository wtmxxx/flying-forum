package com.atcumt.user.api.client.fallback;

import com.atcumt.model.common.entity.Result;
import com.atcumt.model.oss.vo.FileInfoVO;
import com.atcumt.user.api.client.OssClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class OssClientFallback implements FallbackFactory<OssClient> {
    @Override
    public OssClient create(Throwable cause) {
        return new OssClient() {
            @Override
            public Result<FileInfoVO> uploadAvatar(MultipartFile file) {
                log.error("远程调用OssClient#uploadAvatar方法出现异常", cause);
                throw new RuntimeException(cause);
            }
        };
    }
}
