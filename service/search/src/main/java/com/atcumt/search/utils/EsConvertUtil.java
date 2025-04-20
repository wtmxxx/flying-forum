package com.atcumt.search.utils;

import cn.hutool.core.bean.BeanUtil;
import com.atcumt.common.utils.FileConvertUtil;
import com.atcumt.model.post.enums.PostType;
import com.atcumt.model.search.entity.DiscussionEs;
import com.atcumt.model.search.vo.DiscussionEsVO;
import com.atcumt.model.user.vo.UserInfoSimpleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EsConvertUtil {
    private final FileConvertUtil fileConvertUtil;

    public DiscussionEsVO toDiscussionEsVO(String postId, DiscussionEs discussionEs, Map<String, List<String>> highlight, Map<String, UserInfoSimpleVO> userInfoSimpleVOMap) {
        DiscussionEsVO discussionEsVO = BeanUtil.toBean(discussionEs, DiscussionEsVO.class);
        discussionEsVO.setPostType(PostType.DISCUSSION.getValue());
        discussionEsVO.setPostId(postId);
        discussionEsVO.setExcerpt(ExcerptUtil.getExcerpt(discussionEs.getContent()));
        discussionEsVO.setHighlight(highlight);
        discussionEsVO.setUserInfo(userInfoSimpleVOMap.get(discussionEs.getUserId()));
        discussionEsVO.setMediaFiles(fileConvertUtil.convertToMediaFileVOs(discussionEs.getMediaFiles()));

        return discussionEsVO;
    }
}
