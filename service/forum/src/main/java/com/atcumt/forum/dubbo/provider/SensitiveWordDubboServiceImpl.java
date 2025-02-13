package com.atcumt.forum.dubbo.provider;

import com.atcumt.common.api.forum.sensitive.SensitiveWordDubboService;
import com.atcumt.forum.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
@RequiredArgsConstructor
public class SensitiveWordDubboServiceImpl implements SensitiveWordDubboService {
    private final SensitiveWordService sensitiveWordService;

    @Override
    public boolean contains(String content) {
        return sensitiveWordService.contains(content);
    }
}
