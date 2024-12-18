package com.atcumt.forum.repository;

import com.atcumt.model.forum.entity.PostLike;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PostLikeRepository extends MongoRepository<PostLike, Long> {
}
