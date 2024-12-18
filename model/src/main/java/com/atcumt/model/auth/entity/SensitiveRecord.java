package com.atcumt.model.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SensitiveRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long recordId;
    private String userId;
    private String type;
    private String description;
    private String ip;
    private String region;
    private LocalDateTime recordTime;
}
