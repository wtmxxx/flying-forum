-- GPT数据库表结构

CREATE SCHEMA IF NOT EXISTS gpt;
-- 该Schema包含两个主要表：conversation（对话表）和 msg（消息表），用于存储用户与 GPT 系统的对话及其相关消息。

-- 创建聊天对话表，存储用户与GPT的对话记录
USE gpt;
CREATE TABLE IF NOT EXISTS conversation
(
    id      VARCHAR(36) NOT NULL COMMENT '对话ID，由UUID算法生成'
        PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    title       VARCHAR(255)                       NULL COMMENT '对话标题',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '对话创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '对话更新时间',
    INDEX idx_user_id (user_id) COMMENT '用户ID的索引，用于加速查询'
)
    COMMENT '聊天对话表，存储用户与GPT的对话记录';

-- 创建消息表，存储每条消息的详细信息
USE gpt;
CREATE TABLE IF NOT EXISTS message
(
    id              VARCHAR(36) NOT NULL PRIMARY KEY COMMENT '使用UUID算法生成',
    conversation_id VARCHAR(36) NOT NULL COMMENT '会话ID，标识消息所属的会话',
    role            VARCHAR(50)                                                     NOT NULL COMMENT '消息发送者的角色，例如用户或系统',
    content         TEXT                                                            NOT NULL COMMENT '消息的文本内容',
    citations       JSON                                                            NULL COMMENT '引用的内容，保存为JSON格式，可能为空',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP                             NOT NULL COMMENT '消息创建时间，默认为当前时间',
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '消息更新时间，默认为当前时间，更新时自动更新',
    INDEX idx_conversation_id (conversation_id) COMMENT 'conversation_id的索引，用于加速查询'
) COMMENT ='存储每条消息的表，包含内容、角色和引用信息';
