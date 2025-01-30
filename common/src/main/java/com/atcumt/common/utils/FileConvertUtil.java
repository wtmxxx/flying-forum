package com.atcumt.common.utils;

import cn.hutool.json.JSONObject;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.atcumt.model.common.entity.MediaFile;
import com.atcumt.model.common.vo.MediaFileVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@Component
@RequiredArgsConstructor
@ConditionalOnClass(NacosConfigManager.class)
@Slf4j
public class FileConvertUtil {
    private final NacosConfigManager nacosConfigManager;
    public String urlPrefix = "https://kxq.wotemo.com/oss";

    @PostConstruct
    public void initNewsType() {
        String dataId = "oss-url.json";
        String group = "DEFAULT_GROUP";
        try {
            String configInfo = nacosConfigManager.getConfigService()
                    .getConfigAndSignListener(dataId, group, 5000, new Listener() {
                        @Override
                        public void receiveConfigInfo(String configInfo) {
                            log.info("新闻类型配置更新");
                            urlPrefix = new JSONObject(configInfo).getStr("urlPrefix");
                        }

                        @Override
                        public Executor getExecutor() {
                            return null;
                        }
                    });
            urlPrefix = new JSONObject(configInfo).getStr("urlPrefix");
        } catch (NacosException e) {
            log.error("监听新闻类型配置失败");
        }
    }

    public MediaFileVO convertToMediaFileVO(MediaFile mediaFile) {
        if (mediaFile == null) return null;
        return MediaFileVO.builder()
                .url(urlPrefix + "/" + mediaFile.getBucket() + "/" + mediaFile.getFileName())
                .fileType(mediaFile.getFileType())
                .build();
    }

    public String convertToUrl(MediaFile mediaFile) {
        if (mediaFile == null) return null;
        return urlPrefix + "/" + mediaFile.getBucket() + "/" + mediaFile.getFileName();
    }

    public String convertToUrl(String bucket, String fileName) {
        return urlPrefix + "/" + bucket + "/" + fileName;
    }

    public List<MediaFileVO> convertToMediaFileVOs(List<MediaFile> mediaFiles) {
        if (mediaFiles == null) return null;
        List<MediaFileVO> mediaFileVOs = new ArrayList<>();
        for (MediaFile mediaFile : mediaFiles) {
            mediaFileVOs.add(convertToMediaFileVO(mediaFile));
        }
        return mediaFileVOs;
    }
}
