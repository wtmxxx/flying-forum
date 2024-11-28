package com.atcumt.auth.service.impl;

import com.atcumt.auth.service.DisableService;
import com.atcumt.common.utils.DisableUtil;
import com.atcumt.model.auth.dto.DisableServiceBatchDTO;
import com.atcumt.model.auth.dto.UntieDisableServiceBatchDTO;
import com.atcumt.model.auth.vo.DisableTimeVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DisableServiceImpl implements DisableService {
    @Override
    public void disableService(String userId, String service, Long duration) {
        DisableUtil.disableService(userId, service, duration);
    }

    @Override
    public void disableServiceBatch(DisableServiceBatchDTO disableServiceBatchDTO) {
        disableServiceBatchDTO.getServices().forEach(service ->
                DisableUtil.disableService(service.getUserId(), service.getService(), service.getDuration())
        );
    }

    @Override
    public void untieDisableService(String userId, String service) {
        DisableUtil.untieDisableService(userId, service);
    }

    @Override
    public void untieDisableServiceBatch(UntieDisableServiceBatchDTO untieDisableServiceBatchDTO) {
        untieDisableServiceBatchDTO.getServices().forEach(service ->
                DisableUtil.untieDisableService(service.getUserId(), service.getService())
        );
    }

    @Override
    public List<String> getDisableService() {
        return DisableUtil.getAllServices();
    }

    @Override
    public DisableTimeVO getAllDisableServiceTimes(String userId) {
        return DisableTimeVO
                .builder()
                .userId(userId)
                .services(DisableUtil.getAllServiceDisableTimes(userId))
                .build();
    }

    @Override
    public DisableTimeVO getDisableServiceTime(String userId, String service) {
        return DisableTimeVO
                .builder()
                .userId(userId)
                .services(DisableUtil.getServiceDisableTime(userId, service))
                .build();
    }
}
