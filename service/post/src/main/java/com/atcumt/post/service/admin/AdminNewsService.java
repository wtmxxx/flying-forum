package com.atcumt.post.service.admin;

import com.atcumt.model.post.dto.NewsDTO;

import java.util.List;

public interface AdminNewsService {
    void uploadNews(List<NewsDTO> newsDTOs);
}
