package com.atcumt.comment.repository;

import com.atcumt.model.comment.entity.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommentRepository extends MongoRepository<Comment, Long> {
}
