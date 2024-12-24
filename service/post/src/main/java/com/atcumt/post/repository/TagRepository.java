package com.atcumt.post.repository;

import com.atcumt.model.post.entity.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TagRepository extends MongoRepository<Tag, Long> {
}
