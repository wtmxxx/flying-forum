package com.atcumt.search.listener;

import cn.hutool.core.bean.BeanUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.atcumt.model.post.entity.Discussion;
import com.atcumt.model.post.entity.Tag;
import com.atcumt.model.post.enums.PostStatus;
import com.atcumt.model.post.enums.PostType;
import com.atcumt.model.search.dto.SearchPostDTO;
import com.atcumt.model.search.entity.DiscussionEs;
import com.atcumt.model.search.entity.TagSimpleEs;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RocketMQMessageListener(
        topic = "search",
        selectorExpression = "searchPost",
        consumerGroup = "search-post-consumer",
        maxReconsumeTimes = 1
)
@RequiredArgsConstructor
@Slf4j
public class SearchPostConsumer implements RocketMQListener<SearchPostDTO> {
    private final MongoTemplate mongoTemplate;
    private final ElasticsearchClient elasticsearchClient;

    @Override
    public void onMessage(SearchPostDTO searchPostDTO) {
        if (searchPostDTO.getPostId() == null || searchPostDTO.getPostType() == null) {
            log.error("帖子信息不完整，无法处理");
        } else if (PostType.DISCUSSION.getValue().equalsIgnoreCase(searchPostDTO.getPostType())) {
            log.info("新增杂谈索引，帖子ID: {}", searchPostDTO.getPostId());
            insertDiscussion(searchPostDTO);
        } else if (PostType.QUESTION.getValue().equalsIgnoreCase(searchPostDTO.getPostType())) {
            log.info("新增问答索引，帖子ID: {}", searchPostDTO.getPostId());
        } else {
            log.error("未知的帖子类型，无法处理, 帖子类型: {}, 帖子ID: {}", searchPostDTO.getPostType(), searchPostDTO.getPostId());
        }
    }

    @SneakyThrows
    public void insertDiscussion(SearchPostDTO searchPostDTO) {
        Discussion discussion = mongoTemplate.findOne(new Query(Criteria
                .where("_id").is(searchPostDTO.getPostId())), Discussion.class);

        if (discussion == null || PostStatus.DELETED.getCode().equalsIgnoreCase(discussion.getStatus())) {
            log.info("帖子已删除，帖子ID: {}", searchPostDTO.getPostId());
            elasticsearchClient.delete(dr -> dr
                    .index("discussion")
                    .id(String.valueOf(searchPostDTO.getPostId())));
        } else {
            List<Tag> tags = null;
            if (discussion.getTagIds() != null && !discussion.getTagIds().isEmpty()) {
                tags = mongoTemplate.find(new Query(Criteria.where("_id").in(discussion.getTagIds())), Tag.class);
            }
            DiscussionEs discussionEs = DiscussionEs.builder()
                    .userId(discussion.getUserId())
                    .title(discussion.getTitle())
                    .content(discussion.getContent())
                    .mediaFiles(discussion.getMediaFiles())
                    .tags(BeanUtil.copyToList(tags, TagSimpleEs.class))
                    .likeCount(discussion.getLikeCount())
                    .commentCount(discussion.getCommentCount())
                    .status(discussion.getStatus())
                    .score(discussion.getScore())
                    .createTime(discussion.getCreateTime())
                    .updateTime(discussion.getUpdateTime())
                    .build();

            elasticsearchClient.index(ur -> ur
                    .index("discussion")
                    .id(String.valueOf(searchPostDTO.getPostId()))
                    .document(discussionEs));
        }
    }
}
