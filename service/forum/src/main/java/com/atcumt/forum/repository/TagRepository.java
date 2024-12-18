package com.atcumt.forum.repository;

import com.atcumt.model.forum.entity.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TagRepository extends MongoRepository<Tag, Long> {
}
