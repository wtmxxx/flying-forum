package com.atcumt.auth.service;

import com.atcumt.model.auth.dto.DisableServiceBatchDTO;
import com.atcumt.model.auth.dto.UntieDisableServiceBatchDTO;
import com.atcumt.model.auth.vo.DisableTimeVO;

import java.util.List;

public interface DisableService {
    void disableService(String userId, String service, Long duration);

    void disableServiceBatch(DisableServiceBatchDTO disableServiceBatchDTO);

    void untieDisableService(String userId, String service);

    void untieDisableServiceBatch(UntieDisableServiceBatchDTO untieDisableServiceBatchDTO);

    List<String> getDisableService();

    DisableTimeVO getAllDisableServiceTimes(String userId);

    DisableTimeVO getDisableServiceTime(String userId, String service);
}
