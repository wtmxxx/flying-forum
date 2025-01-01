package com.atcumt.comment.repository;

import com.atcumt.model.comment.entity.Reply;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReplyRepository extends MongoRepository<Reply, Long> {
}
