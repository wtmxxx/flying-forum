package com.atcumt.post.repository;

import com.atcumt.model.post.entity.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TagRepository extends MongoRepository<Tag, Long> {
    List<Tag> findByTagNameIn(List<String> tagNames);
}