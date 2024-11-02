package com.atcumt.user.service;

import com.atcumt.model.user.entity.Auth;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AuthService extends IService<Auth> {
    String registerByStuId(String studentId, String unifiedPassword) throws Exception;
}
