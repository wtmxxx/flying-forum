package com.atcumt.model.forum.sensitive.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SensitiveWordConfig {
    @Builder.Default
    @Schema(description = "忽略大小写", defaultValue = "true")
    private Boolean ignoreCase = true;
    @Builder.Default
    @Schema(description = "忽略半角圆角", defaultValue = "true")
    private Boolean ignoreWidth = true;
    @Builder.Default
    @Schema(description = "忽略数字的写法", defaultValue = "true")
    private Boolean ignoreNumStyle = true;
    @Builder.Default
    @Schema(description = "忽略中文的书写格式", defaultValue = "true")
    private Boolean ignoreChineseStyle = true;
    @Builder.Default
    @Schema(description = "忽略英文的书写格式", defaultValue = "true")
    private Boolean ignoreEnglishStyle = true;
    @Builder.Default
    @Schema(description = "忽略重复词", defaultValue = "false")
    private Boolean ignoreRepeat = false;
    @Builder.Default
    @Schema(description = "是否启用数字检测", defaultValue = "false")
    private Boolean enableNumCheck = false;
    @Builder.Default
    @Schema(description = "是否启用邮箱检测", defaultValue = "false")
    private Boolean enableEmailCheck = false;
    @Builder.Default
    @Schema(description = "是否启用链接检测", defaultValue = "false")
    private Boolean enableUrlCheck = false;
    @Builder.Default
    @Schema(description = "是否启用IPv4检测", defaultValue = "false")
    private Boolean enableIpv4Check = false;
    @Builder.Default
    @Schema(description = "是否启用敏感单词检测", defaultValue = "true")
    private Boolean enableWordCheck = true;
    @Builder.Default
    @Schema(description = "数字检测，自定义指定长度", defaultValue = "8")
    private Integer numCheckLen = 8;
    @Builder.Default
    @Schema(description = "敏感词标签，0. 政治 1. 毒品 2. 色情 3. 赌博 4. 违法", defaultValue = "[1, 2, 3]")
    private List<String> wordTags = List.of("1", "2", "3");
}
