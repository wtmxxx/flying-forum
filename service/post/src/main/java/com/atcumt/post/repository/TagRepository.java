package com.atcumt.post.repository;

import com.atcumt.model.post.entity.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends MongoRepository<Tag, Long> {
    List<Tag> findByTagNameIn(List<String> tagNames);
}