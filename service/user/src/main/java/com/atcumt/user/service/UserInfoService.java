package com.atcumt.user.service;

import com.atcumt.model.user.vo.UserInfoVO;

import java.util.List;

public interface UserInfoService {
    UserInfoVO getUserInfo(String userId);

    void changeNickname(String nickname);

    void changeAvatar(String avatar);

    void changeBanner(String banner);

    void changeBio(String bio);

    void changeGender(Integer gender);

    void changeStatus(List<String> status);
}
