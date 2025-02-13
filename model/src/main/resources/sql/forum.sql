-- 用户数据库表结构

-- 创建用户 Schema
CREATE SCHEMA IF NOT EXISTS forum;

-- 使用 auth 表
USE forum;

-- 敏感词表，用于存储敏感词及其分类
CREATE TABLE IF NOT EXISTS sensitive_word
(
    word_id      VARCHAR(36)                                                        NOT NULL COMMENT '敏感词ID'
        PRIMARY KEY,
    word         VARCHAR(255) UNIQUE                                             NOT NULL COMMENT '敏感词，唯一',
    type         VARCHAR(50)                                                     NOT NULL COMMENT '敏感词类型，例如"DENY", "ALLOW"',
    tag          VARCHAR(50) DEFAULT '全局'                                       NOT NULL COMMENT '敏感词标签，例如"政治", "暴力", "低俗"',
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP                             NOT NULL COMMENT '记录创建时间',
    update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '记录更新时间',

    -- 索引
    INDEX idx_word (word) COMMENT '敏感词索引，加速查询',
    INDEX idx_type (type) COMMENT '敏感词类型索引，加速按类型查询',
    INDEX idx_tag (tag) COMMENT '敏感词标签索引，加速按标签查询'
)
    COMMENT = '敏感词表，存储敏感词及其分类信息';
