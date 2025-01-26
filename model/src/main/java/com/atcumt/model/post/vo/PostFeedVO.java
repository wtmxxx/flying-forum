package com.atcumt.model.post.vo;

import cn.hutool.json.JSONObject;
import com.atcumt.model.user.vo.UserInfoSimpleVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostFeedVO {
    private String postType;
    private Long postId;
    private JSONObject postInfo;
    private UserInfoSimpleVO userInfo;
}
