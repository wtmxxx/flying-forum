package com.atcumt.forum.service.impl.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.atcumt.common.enums.PermAction;
import com.atcumt.common.enums.PermModule;
import com.atcumt.common.utils.PermissionUtil;
import com.atcumt.forum.repository.DiscussionRepository;
import com.atcumt.forum.service.admin.AdminDiscussionService;
import com.atcumt.model.forum.dto.DiscussionUpdateDTO;
import com.atcumt.model.forum.entity.Discussion;
import com.atcumt.model.forum.enums.PostStatus;
import com.atcumt.model.forum.vo.DiscussionPostVO;
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

        Integer status = discussionUpdateDTO.getIsDraft() ? 0 : 2; // 草稿为 0，发布为 2

        if (discussionUpdateDTO.getTitle() != null) {
            update.set("title", discussionUpdateDTO.getTitle());
        }
        if (discussionUpdateDTO.getContent() != null) {
            update.set("content", discussionUpdateDTO.getContent());
        }
        if (discussionUpdateDTO.getMediaFiles() != null) {
            update.set("mediaFiles", discussionUpdateDTO.getMediaFiles());

            // 暂时不删除文件
//            List<MediaFile> mediaFiles = discussionRepository
//                    .findMediaFilesByDiscussionId(discussionUpdateDTO.getDiscussionId())
//                    .getMediaFiles();
//            // 如果图片的内容更改，异步删除之前的图片
//            if (mediaFiles != null && !mediaFiles.isEmpty()) {
//                asyncDeleteFile(mediaFiles);
//            }
        }
        if (discussionUpdateDTO.getTagIds() != null) {
            update.set("tags", discussionUpdateDTO.getTagIds());
        }
        if (discussionUpdateDTO.getIsDraft() != null) {
            update.set("status", status);
        } else {
            update.set("createTime", LocalDateTime.now());  // 更新创建时间
        }

        update.set("updateTime", LocalDateTime.now());  // 更新修改时间

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

        if (discussion == null || discussion.getAuthorId() == null) {
            throw new IllegalArgumentException("无此帖子");
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
