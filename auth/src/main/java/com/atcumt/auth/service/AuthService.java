package com.atcumt.auth.service;

import com.atcumt.model.auth.entity.UserAuth;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AuthService extends IService<UserAuth> {
    String registerBySchool(String schoolToken) throws Exception;
}
