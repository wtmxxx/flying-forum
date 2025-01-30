package com.atcumt.user.service;

import com.atcumt.model.user.entity.UserStatus;
import com.atcumt.model.user.vo.UserInfoOtherVO;
import com.atcumt.model.user.vo.UserInfoVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface UserInfoService {
    UserInfoVO getMyUserInfo() throws ExecutionException, InterruptedException;

    UserInfoOtherVO getOtherUserInfo(String userId) throws ExecutionException, InterruptedException;

    void changeNickname(String nickname);

    void changeAvatar(MultipartFile file);

    void changeBio(String bio);

    void changeGender(Integer gender);

    void changeHometown(String hometown);

    void changeMajor(String major);

    void changeGrade(Integer grade);

    void changeStatuses(List<UserStatus> status);
}
