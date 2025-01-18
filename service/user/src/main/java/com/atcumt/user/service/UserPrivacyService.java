package com.atcumt.user.service;

import com.atcumt.model.user.dto.UserPrivacyDTO;
import com.atcumt.model.user.vo.UserPrivacyVO;

public interface UserPrivacyService {
    void setPrivacyLevel(UserPrivacyDTO userPrivacyDTO);

    UserPrivacyVO getPrivacyLevel(String privacyScope);
}
