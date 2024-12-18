package com.atcumt.forum.repository;

import com.atcumt.model.forum.entity.Discussion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface DiscussionRepository extends MongoRepository<Discussion, Long> {
    // 你可以在这里定义自己的查询方法
    // 例如：根据状态查询讨论
    List<Discussion> findByStatus(Integer status);

    @Query(value = "{ '_id': ?0 }", fields = "{ 'mediaFiles': 1, '_id': 0 }")
        // 投影，仅返回 mediaFiles 字段
    Discussion findMediaFilesByDiscussionId(Long discussionId);

    @Query(value = "{ '_id': ?0 }", fields = "{ 'authorId': 1, '_id': 0 }")
        // 投影，仅返回 mediaFiles 字段
    Discussion findAuthorIdByDiscussionId(Long discussionId);
}
