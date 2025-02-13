package com.atcumt.forum.service.impl;

import com.atcumt.forum.mapper.SensitiveWordMapper;
import com.atcumt.forum.service.SensitiveWordService;
import com.atcumt.model.forum.sensitive.entity.SensitiveWord;
import com.atcumt.model.forum.sensitive.vo.SensitiveWordFindVO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SensitiveWordServiceImpl extends ServiceImpl<SensitiveWordMapper, SensitiveWord> implements SensitiveWordService {
    private final SensitiveWordBs sensitiveWordBs;

    @Override
    public boolean contains(String content) {
        return sensitiveWordBs.contains(content);
    }

    @Override
    public SensitiveWordFindVO find(String content, Boolean findFirst) {
        if (findFirst) {
            String word = sensitiveWordBs.findFirst(content);
            if (word == null) {
                return SensitiveWordFindVO
                        .builder()
                        .contains(false)
                        .build();
            } else {
                return SensitiveWordFindVO
                        .builder()
                        .contains(true)
                        .words(List.of(word))
                        .build();
            }
        } else {
            List<String> words = sensitiveWordBs.findAll(content);
            if (words.isEmpty()) {
                return SensitiveWordFindVO
                        .builder()
                        .contains(false)
                        .build();
            } else {
                return SensitiveWordFindVO
                        .builder()
                        .contains(true)
                        .words(words)
                        .build();
            }
        }
    }
}
