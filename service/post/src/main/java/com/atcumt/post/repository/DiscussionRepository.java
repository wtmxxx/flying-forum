package com.atcumt.post.repository;

import com.atcumt.model.post.entity.Discussion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionRepository extends MongoRepository<Discussion, Long> {
    // 你可以在这里定义自己的查询方法
    // 例如：根据状态查询讨论
    List<Discussion> findByStatus(String status);

    @Query(value = "{ '_id': ?0 }", fields = "{ 'mediaFiles': 1, '_id': 0 }")
        // 投影，仅返回 mediaFiles 字段
    Discussion findMediaFilesByDiscussionId(Long discussionId);

    @Query(value = "{ '_id': ?0 }", fields = "{ 'userId': 1, '_id': 0 }")
        // 投影，仅返回 mediaFiles 字段
    Discussion findUserIdIdByDiscussionId(Long discussionId);

    @Query(value = "{ 'tagIds': ?0 }", fields = "{ '_id': 1 }")
    List<Discussion> findDiscussionIdsInTagIds(Long tagId);
}
