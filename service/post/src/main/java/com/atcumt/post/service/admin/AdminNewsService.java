package com.atcumt.post.service.admin;

import com.alibaba.nacos.api.exception.NacosException;
import com.atcumt.model.post.dto.NewsDTO;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface AdminNewsService {
    void uploadNews(List<NewsDTO> newsDTOs);

    void uploadNewsType(JsonNode newsType) throws NacosException;
}
