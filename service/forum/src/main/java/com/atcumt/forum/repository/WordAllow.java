package com.atcumt.forum.repository;

import com.atcumt.forum.mapper.SensitiveWordMapper;
import com.atcumt.model.forum.sensitive.entity.SensitiveWord;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.houbb.sensitive.word.api.IWordAllow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WordAllow implements IWordAllow {
    private final SensitiveWordMapper sensitiveWordMapper;

    @Override
    public List<String> allow() {
        List<String> sensitiveWords = sensitiveWordMapper.selectObjs(Wrappers
                .<SensitiveWord>lambdaQuery()
                .select(SensitiveWord::getWord)
                .eq(SensitiveWord::getType, "ALLOW"));

        return sensitiveWords;
    }
}
