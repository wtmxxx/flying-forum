package com.atcumt.model.forum.sensitive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SensitiveWord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long wordId;    // 敏感词ID
    private String word;    // 敏感词
    private String type;    // 类型
    private String tag;     // 标签
    private String createTime;  // 创建时间
    private String updateTime;  // 更新时间
}
