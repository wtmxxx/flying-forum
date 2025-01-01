package com.atcumt.post.service.impl.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.atcumt.common.enums.PermAction;
import com.atcumt.common.enums.PermModule;
import com.atcumt.common.utils.PermissionUtil;
import com.atcumt.model.post.dto.DiscussionUpdateDTO;
import com.atcumt.model.post.entity.Discussion;
import com.atcumt.model.post.enums.PostMessage;
import com.atcumt.model.post.enums.PostStatus;
import com.atcumt.model.post.vo.DiscussionPostVO;
import com.atcumt.post.repository.DiscussionRepository;
import com.atcumt.post.service.admin.AdminDiscussionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminDiscussionServiceImpl implements AdminDiscussionService {
    private final MongoTemplate mongoTemplate;
    private final DiscussionRepository discussionRepository;

    @Override
    public DiscussionPostVO updateDiscussion(DiscussionUpdateDTO discussionUpdateDTO) {
        // 检查权限
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.DISCUSSION, PermAction.UPDATE));

        // 创建Update对象，只更新非null字段
        Update update = new Update();

        if (discussionUpdateDTO.getTitle() != null) {
            update.set("title", discussionUpdateDTO.getTitle());
        }
        if (discussionUpdateDTO.getContent() != null) {
            update.set("content", discussionUpdateDTO.getContent());
        }
        if (discussionUpdateDTO.getMediaFiles() != null) {
            update.set("mediaFiles", discussionUpdateDTO.getMediaFiles());
        }
        if (discussionUpdateDTO.getTagIds() != null) {
            update.set("tagIds", discussionUpdateDTO.getTagIds());
        }
        // 设置状态为已发布
        Integer status = PostStatus.PUBLISHED.getCode();
        update.set("status", status);
        update.set("updateTime", LocalDateTime.now());

        // 使用MongoTemplate执行部分更新
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("discussionId").is(discussionUpdateDTO.getDiscussionId())),
                update,
                Discussion.class
        );

        return DiscussionPostVO
                .builder()
                .discussionId(discussionUpdateDTO.getDiscussionId())
                .status(status)
                .build();
    }

    @Override
    public void deleteDiscussion(Long discussionId) {
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.DISCUSSION, PermAction.DELETE));
        Discussion discussion = discussionRepository.findById(discussionId).orElse(null);

        if (discussion == null || discussion.getUserId() == null) {
            throw new IllegalArgumentException(PostMessage.POST_NOT_FOUND.getMessage());
        }

        Update update = new Update();
        update.set("status", PostStatus.DELETED.getCode());  // 软删除
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("discussionId").is(discussionId)),
                update,
                Discussion.class
        );
    }

    @Override
    public void deleteDiscussionComplete(Long discussionId) {
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.DISCUSSION, PermAction.DELETE));

        // TODO 完全删除帖子，有待研究
    }
}
