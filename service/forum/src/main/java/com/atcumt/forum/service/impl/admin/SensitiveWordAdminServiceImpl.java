package com.atcumt.forum.service.impl.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.atcumt.common.enums.PermAction;
import com.atcumt.common.enums.PermModule;
import com.atcumt.common.utils.PermissionUtil;
import com.atcumt.forum.mapper.SensitiveWordMapper;
import com.atcumt.forum.repository.WordAllow;
import com.atcumt.forum.repository.WordDeny;
import com.atcumt.forum.service.admin.SensitiveWordAdminService;
import com.atcumt.model.forum.sensitive.dto.SensitiveWordDTO;
import com.atcumt.model.forum.sensitive.dto.SensitiveWordListDTO;
import com.atcumt.model.forum.sensitive.entity.SensitiveWord;
import com.atcumt.model.forum.sensitive.entity.SensitiveWordConfig;
import com.atcumt.model.forum.sensitive.vo.SensitiveWordVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.allow.WordAllows;
import com.github.houbb.sensitive.word.support.deny.WordDenys;
import com.github.houbb.sensitive.word.support.ignore.SensitiveWordCharIgnores;
import com.github.houbb.sensitive.word.support.resultcondition.WordResultConditions;
import com.github.houbb.sensitive.word.support.tag.WordTags;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SensitiveWordAdminServiceImpl extends ServiceImpl<SensitiveWordMapper, SensitiveWord> implements SensitiveWordAdminService {
    private final SensitiveWordMapper sensitiveWordMapper;
    private final SensitiveWordBs sensitiveWordBs;
    private final WordAllow wordAllow;
    private final WordDeny wordDeny;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void addSensitiveWord(SensitiveWordListDTO sensitiveWordListDTO) {
        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.SENSITIVE_WORD, PermAction.CREATE));

        List<SensitiveWord> sensitiveWords = new ArrayList<>();
        List<String> wordAllow = new ArrayList<>();
        List<String> wordDeny = new ArrayList<>();

        for (SensitiveWordDTO word : sensitiveWordListDTO.getWords()) {
            SensitiveWord sensitiveWord = SensitiveWord
                    .builder()
                    .word(word.getWord().toLowerCase())
                    .type("ALLOW".equals(word.getType()) ? "ALLOW" : "DENY")
                    .tag(word.getTag())
                    .build();
            sensitiveWords.add(sensitiveWord);
            if ("ALLOW".equals(word.getType())) {
                wordAllow.add(word.getWord());
            } else {
                wordDeny.add(word.getWord());
            }
        }

        try {
            sensitiveWordMapper.insert(sensitiveWords);
        } catch (Exception e) {
            throw new IllegalArgumentException("部分敏感词已存在");
        }

        if (!wordAllow.isEmpty()) {
            sensitiveWordBs.addWordAllow(wordAllow);
        }
        if (!wordDeny.isEmpty()) {
            sensitiveWordBs.addWord(wordDeny);
        }

        sensitiveWordBs.removeWord(wordDeny);
    }

    @Override
    public void removeSensitiveWord(List<String> words) {
        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.SENSITIVE_WORD, PermAction.DELETE));


        if (words == null || words.isEmpty()) {
            return;
        }
        List<SensitiveWord> sensitiveWords = sensitiveWordMapper.selectList(Wrappers
                .<SensitiveWord>lambdaQuery()
                .in(SensitiveWord::getWord, words));

        List<String> wordAllow = new ArrayList<>();
        List<String> wordDeny = new ArrayList<>();
        for (SensitiveWord sensitiveWord : sensitiveWords) {
            if ("ALLOW".equals(sensitiveWord.getType())) {
                wordAllow.add(sensitiveWord.getWord().toLowerCase());
            } else {
                wordDeny.add(sensitiveWord.getWord().toLowerCase());
            }
        }

        sensitiveWordBs.removeWordAllow(wordAllow);
        sensitiveWordBs.removeWord(wordDeny);

        sensitiveWordMapper.delete(Wrappers
                .<SensitiveWord>lambdaQuery()
                .in(SensitiveWord::getWord, words));
    }

    @Override
    public List<SensitiveWordVO> getSensitiveWords(String type, String tag) {
        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.SENSITIVE_WORD, PermAction.READ));

        return sensitiveWordMapper.selectList(Wrappers
                .<SensitiveWord>lambdaQuery()
                .eq(!"ALL".equals(type), SensitiveWord::getType, type)
                .eq(!"ALL".equals(tag), SensitiveWord::getTag, tag))
                .stream()
                .map(sensitiveWord -> SensitiveWordVO
                        .builder()
                        .wordId(sensitiveWord.getWordId())
                        .word(sensitiveWord.getWord())
                        .type(sensitiveWord.getType())
                        .tag(sensitiveWord.getTag())
                        .createTime(sensitiveWord.getCreateTime())
                        .updateTime(sensitiveWord.getUpdateTime())
                        .build())
                .toList();
    }

    @Override
    public void configSensitiveWord(SensitiveWordConfig sensitiveWordConfig) {
        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.SENSITIVE_WORD, PermAction.CREATE));

        redisTemplate.opsForValue().set("forum:sensitive-word:config", sensitiveWordConfig);
        sensitiveWordBs
                .ignoreCase(sensitiveWordConfig.getIgnoreCase())
                .ignoreWidth(sensitiveWordConfig.getIgnoreWidth())
                .ignoreNumStyle(sensitiveWordConfig.getIgnoreNumStyle())
                .ignoreChineseStyle(sensitiveWordConfig.getIgnoreChineseStyle())
                .ignoreEnglishStyle(sensitiveWordConfig.getIgnoreEnglishStyle())
                .ignoreRepeat(sensitiveWordConfig.getIgnoreRepeat())
                .enableNumCheck(sensitiveWordConfig.getEnableNumCheck())
                .enableEmailCheck(sensitiveWordConfig.getEnableEmailCheck())
                .enableUrlCheck(sensitiveWordConfig.getEnableUrlCheck())
                .enableIpv4Check(sensitiveWordConfig.getEnableIpv4Check())
                .enableWordCheck(sensitiveWordConfig.getEnableWordCheck())
                .numCheckLen(sensitiveWordConfig.getNumCheckLen())
                .wordTag(WordTags.defaults())
                .charIgnore(SensitiveWordCharIgnores.specialChars())
                .wordResultCondition(WordResultConditions.chains(
                        WordResultConditions.englishWordMatch(),
                        // 0. 政治 1. 毒品 2. 色情 3. 赌博 4. 违法
                        WordResultConditions.wordTags(sensitiveWordConfig.getWordTags())
                ))
                .wordAllow(WordAllows.chains(WordAllows.defaults(), wordAllow))
                .wordDeny(WordDenys.chains(WordDenys.defaults(), wordDeny))
                // 各种其他配置
                .init();
    }

    @Override
    public SensitiveWordConfig getSensitiveWordConfig() {
        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.SENSITIVE_WORD, PermAction.READ));

        SensitiveWordConfig sensitiveWordConfig = (SensitiveWordConfig) redisTemplate.opsForValue().get("forum:sensitive-word:config");

        if (sensitiveWordConfig == null) {
            sensitiveWordConfig = new SensitiveWordConfig();
            redisTemplate.opsForValue().set("forum:sensitive-word:config", sensitiveWordConfig);
        }

        return sensitiveWordConfig;
    }

    @Override
    public Set<String> getSensitiveWordTags(String word) {
        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.SENSITIVE_WORD, PermAction.READ));

        return sensitiveWordBs.tags(word);
    }
}
