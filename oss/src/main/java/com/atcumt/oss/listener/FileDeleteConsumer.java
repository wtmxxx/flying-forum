package com.atcumt.oss.listener;

import com.atcumt.model.oss.dto.FileInfoDTO;
import com.atcumt.oss.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(
        topic = "oss",
        selectorExpression = "file-delete",
        consumerGroup = "file-delete-consumer",
        maxReconsumeTimes = 3
)
@RequiredArgsConstructor
@Slf4j
public class FileDeleteConsumer implements RocketMQListener<FileInfoDTO> {
    private final FileService fileService;

    @Override
    public void onMessage(FileInfoDTO fileInfoDTO) {
        String bucket = fileInfoDTO.getBucket();
        String filename = fileInfoDTO.getFileName();

        log.info("删除文件, bucket: {}, filename: {}", bucket, filename);
        try {
            fileService.deleteFile(FileInfoDTO
                    .builder()
                    .bucket(bucket)
                    .fileName(filename)
                    .build());
        } catch (Exception e) {
            log.error("删除文件失败, bucket: {}, filename: {}", bucket, filename, e);
        }
    }
}
