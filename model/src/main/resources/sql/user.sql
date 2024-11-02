-- 用户数据库表结构

-- 创建用户 Schema
CREATE SCHEMA IF NOT EXISTS user;

-- 创建用户认证表，用于存储用户的登录和认证信息
USE user;
CREATE TABLE IF NOT EXISTS auth
(
    id              VARCHAR(36)                                                     NOT NULL COMMENT 'AuthID，由UUID生成'
        PRIMARY KEY,
    student_id      VARCHAR(25) UNIQUE COMMENT '学生学号，用户的校园唯一标识',
    username        VARCHAR(50) UNIQUE                                              NULL COMMENT '系统内唯一用户名，用于登录',
    email           VARCHAR(100) UNIQUE                                             NULL COMMENT '用户邮箱，唯一，用于找回密码',
    password        VARCHAR(255)                                                    NULL COMMENT '加密存储的用户密码',
    qq_oauth_id     VARCHAR(50)                                                     NULL COMMENT 'QQ的OAuth 2.0认证ID，绑定QQ时使用',
    wechat_oauth_id VARCHAR(50)                                                     NULL COMMENT '微信的OAuth 2.0认证ID，绑定微信时使用',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP                             NOT NULL COMMENT '用户注册时间，默认为当前时间',
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '用户信息更新时间，更新时自动刷新',
    -- 为学号、用户名、邮箱及OAuth ID添加索引
    INDEX idx_student_id (student_id) COMMENT '学生学号的索引，加速按学号查询',
    INDEX idx_username (username) COMMENT '用户名的索引，加速按用户名查询',
    INDEX idx_email (email) COMMENT '用户邮箱的索引，加速按邮箱查询',
    INDEX idx_qq_oauth_id (qq_oauth_id) COMMENT 'QQ OAuth ID的索引，加速按QQ OAuth ID查询',
    INDEX idx_wechat_oauth_id (wechat_oauth_id) COMMENT '微信 OAuth ID的索引，加速按微信 OAuth ID查询'
)
    COMMENT = '用户认证表，存储用户的基本认证信息，包括用户名、邮箱及第三方OAuth信息';
