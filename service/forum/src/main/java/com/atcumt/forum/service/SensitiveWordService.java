package com.atcumt.forum.service;

import com.atcumt.model.forum.sensitive.entity.SensitiveWord;
import com.atcumt.model.forum.sensitive.vo.SensitiveWordFindVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SensitiveWordService extends IService<SensitiveWord> {
    boolean contains(String content);

    SensitiveWordFindVO find(String content, Boolean findFirst);
}
