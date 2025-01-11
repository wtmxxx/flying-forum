package com.atcumt.like.repository;

import com.atcumt.model.like.entity.PostLike;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PostLikeRepository extends MongoRepository<PostLike, Long> {
}
