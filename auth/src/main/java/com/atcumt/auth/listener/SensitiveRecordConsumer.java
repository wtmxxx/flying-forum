package com.atcumt.auth.listener;

import com.atcumt.auth.mapper.SensitiveRecordMapper;
import com.atcumt.model.auth.entity.SensitiveRecord;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RocketMQMessageListener(
        topic = "auth",
        selectorExpression = "sensitiveRecord",
        consumerGroup = "auth-consumer"
)
@RequiredArgsConstructor
@Slf4j
public class SensitiveRecordConsumer implements RocketMQListener<SensitiveRecord> {
    private final SensitiveRecordMapper sensitiveRecordMapper;

    @Override
    public void onMessage(SensitiveRecord sensitiveRecord) {
        sensitiveRecordMapper.delete(Wrappers
                .<SensitiveRecord>lambdaQuery()
//                .eq(SensitiveRecord::getUserId, sensitiveRecord.getUserId())
                .lt(SensitiveRecord::getRecordTime, LocalDateTime.now().minusDays(30))
        );

        sensitiveRecordMapper.insert(sensitiveRecord);
    }
}
