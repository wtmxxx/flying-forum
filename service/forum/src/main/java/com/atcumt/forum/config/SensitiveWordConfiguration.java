package com.atcumt.forum.config;

import com.atcumt.forum.repository.WordAllow;
import com.atcumt.forum.repository.WordDeny;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.allow.WordAllows;
import com.github.houbb.sensitive.word.support.check.WordChecks;
import com.github.houbb.sensitive.word.support.deny.WordDenys;
import com.github.houbb.sensitive.word.support.ignore.SensitiveWordCharIgnores;
import com.github.houbb.sensitive.word.support.resultcondition.WordResultConditions;
import com.github.houbb.sensitive.word.support.tag.WordTags;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SensitiveWordConfiguration {
    private final WordAllow wordAllow;
    private final WordDeny wordDeny;

    /**
     * 初始化引导类
     * @return 初始化引导类
     * @since 1.0.0
     */
    @Bean
    public SensitiveWordBs sensitiveWordBs() {
        SensitiveWordBs sensitiveWordBs = SensitiveWordBs.newInstance()
                .ignoreCase(true)
                .ignoreWidth(true)
                .ignoreNumStyle(true)
                .ignoreChineseStyle(true)
                .ignoreEnglishStyle(true)
                .ignoreRepeat(false)
                .enableNumCheck(false)
                .enableEmailCheck(false)
                .enableUrlCheck(false)
                .wordCheckUrl(WordChecks.urlNoPrefix())
                .enableIpv4Check(false)
                .enableWordCheck(true)
                .numCheckLen(8)
                .wordTag(WordTags.defaults())
                .charIgnore(SensitiveWordCharIgnores.specialChars())
                .wordResultCondition(WordResultConditions.chains(
                        WordResultConditions.englishWordMatch(),
                        WordResultConditions.wordTags(List.of("1", "2", "3")) // 0. 政治 1. 毒品 2. 色情 3. 赌博 4. 违法
                ))
                .wordAllow(WordAllows.chains(WordAllows.defaults(), wordAllow))
                .wordDeny(WordDenys.chains(WordDenys.defaults(), wordDeny))
                // 各种其他配置
                .init();
        return sensitiveWordBs;
    }
}
