package com.atcumt.user.service;

import com.atcumt.model.user.dto.UserFollowerDTO;
import com.atcumt.model.user.dto.UserFollowingDTO;
import com.atcumt.model.user.vo.UserFollowerVO;
import com.atcumt.model.user.vo.UserFollowingVO;

public interface UserFollowService {
    void followUser(String followedId);

    UserFollowingVO getUserFollowings(UserFollowingDTO userFollowingDTO) throws InterruptedException;

    UserFollowerVO getUserFollowers(UserFollowerDTO userFollowerDTO);
}
