package com.atcumt.model.post.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiscussionPostVO {
    private Long discussionId; // 雪花算法生成ID，自动作为主键，MongoDB会为其创建索引
    private Integer status;  // 帖子状态（0 - 草稿，1 - 审核中，2 - 已发布，3 - 审核不通过，-1 - 已删除）
}
