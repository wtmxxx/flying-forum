package com.atcumt.post.service.impl.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.atcumt.common.enums.PermAction;
import com.atcumt.common.enums.PermModule;
import com.atcumt.common.utils.PermissionUtil;
import com.atcumt.model.post.dto.QuestionUpdateDTO;
import com.atcumt.model.post.entity.Question;
import com.atcumt.model.post.enums.PostMessage;
import com.atcumt.model.post.enums.PostStatus;
import com.atcumt.model.post.vo.QuestionPostVO;
import com.atcumt.post.repository.QuestionRepository;
import com.atcumt.post.service.admin.AdminQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminQuestionServiceImpl implements AdminQuestionService {
    private final MongoTemplate mongoTemplate;
    private final QuestionRepository questionRepository;

    @Override
    public QuestionPostVO updateQuestion(QuestionUpdateDTO questionUpdateDTO) {
        // 检查权限
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.DISCUSSION, PermAction.UPDATE));

        // 创建Update对象，只更新非null字段
        Update update = new Update();

        if (questionUpdateDTO.getTitle() != null) {
            update.set("title", questionUpdateDTO.getTitle());
        }
        if (questionUpdateDTO.getContent() != null) {
            update.set("content", questionUpdateDTO.getContent());
        }
        if (questionUpdateDTO.getMediaFiles() != null) {
            update.set("mediaFiles", questionUpdateDTO.getMediaFiles());
        }
        if (questionUpdateDTO.getTagIds() != null) {
            update.set("tagIds", questionUpdateDTO.getTagIds());
        }
        // 设置状态为已发布
        String status = PostStatus.PUBLISHED.getCode();
        update.set("status", status);
        update.set("updateTime", LocalDateTime.now());

        // 使用MongoTemplate执行部分更新
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("questionId").is(questionUpdateDTO.getQuestionId())),
                update,
                Question.class
        );

        return QuestionPostVO
                .builder()
                .questionId(questionUpdateDTO.getQuestionId())
                .status(status)
                .build();
    }

    @Override
    public void deleteQuestion(Long questionId) {
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.DISCUSSION, PermAction.DELETE));
        Question question = questionRepository.findById(questionId).orElse(null);

        if (question == null || question.getUserId() == null) {
            throw new IllegalArgumentException(PostMessage.POST_NOT_FOUND.getMessage());
        }

        Update update = new Update();
        update.set("status", PostStatus.DELETED.getCode());  // 软删除
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("questionId").is(questionId)),
                update,
                Question.class
        );
    }

    @Override
    public void deleteQuestionComplete(Long questionId) {
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.DISCUSSION, PermAction.DELETE));

        // TODO 完全删除帖子，有待研究
    }
}
