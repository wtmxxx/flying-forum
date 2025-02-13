package com.atcumt.forum.repository;

import com.atcumt.forum.mapper.SensitiveWordMapper;
import com.atcumt.model.forum.sensitive.entity.SensitiveWord;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.houbb.sensitive.word.api.IWordDeny;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WordDeny implements IWordDeny {
    private final SensitiveWordMapper sensitiveWordMapper;

    @Override
    public List<String> deny() {
        List<String> sensitiveWords = sensitiveWordMapper.selectObjs(Wrappers
                .<SensitiveWord>lambdaQuery()
                .select(SensitiveWord::getWord)
                .eq(SensitiveWord::getType, "DENY"));

        return sensitiveWords;
    }
}
