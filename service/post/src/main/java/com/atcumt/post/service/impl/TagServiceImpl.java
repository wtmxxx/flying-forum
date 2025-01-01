package com.atcumt.post.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.atcumt.model.post.dto.NewTagDTO;
import com.atcumt.model.post.entity.Tag;
import com.atcumt.model.post.enums.PostMessage;
import com.atcumt.model.post.vo.TagListVO;
import com.atcumt.model.post.vo.TagVO;
import com.atcumt.post.repository.TagRepository;
import com.atcumt.post.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {
    private final TagRepository tagRepository;

    @Override
    public TagListVO newTag(NewTagDTO newTagDTO) {
        // 获取前端传递的标签名称列表
        List<String> tagNames = newTagDTO.getTagNames();
        if (tagNames == null || tagNames.isEmpty()) {
            throw new IllegalArgumentException(PostMessage.TAG_LIST_EMPTY.getMessage());
        }

        if (tagNames.size() > 20) {
            throw new IllegalArgumentException(PostMessage.TAG_COUNT_TOO_MANY.getMessage());
        }

        // 数据库中已存在的标签
        List<Tag> existingTags = tagRepository.findByTagNameIn(tagNames);
        Set<String> existingTagNames = existingTags.stream()
                .map(Tag::getTagName)
                .collect(Collectors.toSet());

        // 找出不存在于数据库中的标签名称
        List<Tag> newTags = tagNames.stream()
                .filter(tagName -> !existingTagNames.contains(tagName))
                .map(tagName ->
                        Tag.builder()
                                .tagId(IdUtil.getSnowflakeNextId())
                                .tagName(tagName)
                                .usageCount(0L)
                                .viewCount(0L)
                                .createTime(LocalDateTime.now())
                                .build()
                ) // 将标签名称映射为 Tag 实体
                .collect(Collectors.toList());

        // 批量插入不存在的标签
        if (!newTags.isEmpty()) {
            tagRepository.saveAll(newTags);
        }

        // 添加新标签
        existingTags.addAll(newTags);

        // 将现有标签按传递的 tagNames 顺序重新排列
        Map<String, Tag> tagNameToTagMap = existingTags.stream()
                .collect(Collectors.toMap(Tag::getTagName, tag -> tag));

        // 合并新标签和已存在标签，并保持顺序
        List<Tag> allTags = new ArrayList<>();

        // 首先按照传入的 tagNames 顺序添加已存在标签
        tagNames.forEach(tagName -> {
            if (tagNameToTagMap.containsKey(tagName)) {
                allTags.add(tagNameToTagMap.get(tagName));
            }
        });

        // 封装返回结果为 VO
        TagListVO tagListVO = new TagListVO();
        tagListVO.setTags(BeanUtil.copyToList(allTags, TagVO.class));
        return tagListVO;
    }

    @Override
    public TagVO getTag(Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new IllegalArgumentException(PostMessage.TAG_NOT_FOUND.getMessage()));
        return BeanUtil.copyProperties(tag, TagVO.class);
    }

}
