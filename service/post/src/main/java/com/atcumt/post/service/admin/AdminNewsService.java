package com.atcumt.post.service.admin;

import cn.hutool.json.JSONObject;
import com.alibaba.nacos.api.exception.NacosException;
import com.atcumt.model.post.dto.NewsDTO;

import java.util.List;

public interface AdminNewsService {
    void uploadNews(List<NewsDTO> newsDTOs);

    void uploadNewsType(JSONObject newsType) throws NacosException;
}
