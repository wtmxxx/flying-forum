package com.atcumt.forum.service.admin;

import com.atcumt.model.forum.sensitive.entity.SensitiveWordConfig;
import com.atcumt.model.forum.sensitive.dto.SensitiveWordListDTO;
import com.atcumt.model.forum.sensitive.entity.SensitiveWord;
import com.atcumt.model.forum.sensitive.vo.SensitiveWordVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface SensitiveWordAdminService extends IService<SensitiveWord> {
    void addSensitiveWord(SensitiveWordListDTO sensitiveWordListDTO);

    void removeSensitiveWord(List<String> words);

    List<SensitiveWordVO> getSensitiveWords(String type, String tag);

    void configSensitiveWord(SensitiveWordConfig sensitiveWordConfig);

    SensitiveWordConfig getSensitiveWordConfig();
}
