package com.atcumt.model.ai.enums;

import lombok.Getter;

@Getter
public enum AiStatus {
    /**
     * 未使用
     */
    UNUSED("UNUSED", "未使用"),
    /**
     * 请求已收到，正在排队等待处理
     */
    QUEUED("QUEUED", "排队中"),

    /**
     * 模型正在处理中
     */
    PROCESSING("PROCESSING", "处理中"),

    /**
     * 流式输出中（适用于 Streaming 模型）
     */
    STREAMING("STREAMING", "生成中"),

    /**
     * 模型返回成功，已完成
     */
    FINISHED("FINISHED", "已完成"),

    /**
     * 用户主动取消请求
     */
    CANCELLED("CANCELLED", "用户取消"),

    /**
     * 执行失败，比如模型异常、中断等
     */
    FAILED("FAILED", "生成失败"),

    /**
     * 其他状态
     */
    OTHER("OTHER", "其他"),
    ;

    private final String value;
    private final String description;

    AiStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
}