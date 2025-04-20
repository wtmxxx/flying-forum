package com.atcumt.post.service.impl.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.atcumt.common.enums.PermAction;
import com.atcumt.common.enums.PermModule;
import com.atcumt.common.utils.PermissionUtil;
import com.atcumt.model.post.entity.Discussion;
import com.atcumt.model.post.entity.Tag;
import com.atcumt.model.search.dto.SearchSuggestionDTO;
import com.atcumt.model.search.enums.SuggestionAction;
import com.atcumt.model.search.enums.SuggestionType;
import com.atcumt.post.repository.DiscussionRepository;
import com.atcumt.post.service.admin.AdminTagService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTagServiceImpl implements AdminTagService {
    private final DiscussionRepository discussionRepository;
    private final MongoTemplate mongoTemplate;
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class,
            transactionManager = "mongoTransactionManager",
            label = "mongo:readPreference=PRIMARY")
    public void deleteTag(Long tagId) {
        // 检查权限
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.TAG, PermAction.DELETE));

        Tag tag = mongoTemplate.findAndRemove(new Query(Criteria.where("tagId").is(tagId)), Tag.class);

        List<Long> postIds = discussionRepository
                .findDiscussionIdsInTagIds(tagId)
                .stream().map(Discussion::getDiscussionId)
                .toList();

        // 删除标签后，需要将所有使用该标签的帖子的标签列表中删除该标签
        Query query = new Query(Criteria.where("discussionId").in(postIds));
        Update update = new Update().pull("tagIds", tagId);
        mongoTemplate.updateMulti(query, update, Discussion.class);

        if (tag != null) {
            deleteTagSuggestion(tag);
        }
    }

    @Async
    public void deleteTagSuggestion(Tag tag) {
        SearchSuggestionDTO searchSuggestionDTO = SearchSuggestionDTO
                .builder()
                .action(SuggestionAction.DELETE)
                .suggestion(tag.getTagName())
                .type(SuggestionType.TAG.getValue())
                .build();
        rocketMQTemplate.convertAndSend("search:searchSuggestion", searchSuggestionDTO);
    }
}
