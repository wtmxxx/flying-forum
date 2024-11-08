-- 用户数据库表结构

-- 创建用户 Schema
CREATE SCHEMA IF NOT EXISTS auth;

-- 使用 auth 表
USE auth;

-- 创建用户认证表，用于存储用户的登录和认证信息
CREATE TABLE IF NOT EXISTS user_auth
(
    user_id         VARCHAR(36)                                                     NOT NULL COMMENT 'UserID，由UUID生成'
        PRIMARY KEY,
    student_id      VARCHAR(25) UNIQUE                                              NULL COMMENT '学生学号，用户的校园唯一标识',
    username        VARCHAR(50) UNIQUE                                              NULL COMMENT '系统内唯一用户名，用于登录',
    email           VARCHAR(100) UNIQUE                                             NULL COMMENT '用户邮箱，唯一，用于找回密码',
    password        VARCHAR(255)                                                    NULL COMMENT '加密存储的用户密码',
    qq_oauth_id     VARCHAR(100)                                                    NULL COMMENT 'QQ的OAuth 2.0认证ID，绑定QQ时使用',
    wechat_oauth_id VARCHAR(100)                                                    NULL COMMENT '微信的OAuth 2.0认证ID，绑定微信时使用',
    enabled         BOOLEAN   DEFAULT TRUE                                          NOT NULL COMMENT '账户是否启用，TRUE表示启用，FALSE表示禁用',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP                             NOT NULL COMMENT '用户注册时间，默认为当前时间',
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '用户信息更新时间，更新时自动刷新',
    last_login_time TIMESTAMP DEFAULT NULL                                          NULL COMMENT '用户最后登录时间',

    -- 为学号、用户名、邮箱及OAuth ID添加索引
    INDEX idx_student_id (student_id) COMMENT '学生学号的索引，加速按学号查询',
    INDEX idx_username (username) COMMENT '用户名的索引，加速按用户名查询',
    INDEX idx_email (email) COMMENT '用户邮箱的索引，加速按邮箱查询',
    INDEX idx_qq_oauth_id (qq_oauth_id) COMMENT 'QQ OAuth ID的索引，加速按QQ OAuth ID查询',
    INDEX idx_wechat_oauth_id (wechat_oauth_id) COMMENT '微信 OAuth ID的索引，加速按微信 OAuth ID查询'
)
    COMMENT = '用户认证表，存储用户的基本认证信息，包括用户名、邮箱、启用状态及第三方OAuth信息';


-- 微信授权信息表，用于存储微信授权登录信息
CREATE TABLE IF NOT EXISTS wechat_auth
(
    wechat_auth_id CHAR(36)                                                        NOT NULL COMMENT '记录ID，由UUID生成'
        PRIMARY KEY,
    user_id        CHAR(36)                                                        NOT NULL COMMENT '关联auth表的ID',
    openid         VARCHAR(50)                                                     NOT NULL UNIQUE COMMENT '微信OpenID，用于标识用户在当前应用的唯一标识',
    unionid        VARCHAR(50)                                                     NULL UNIQUE COMMENT '微信UnionID，用于跨应用标识用户的唯一标识（同一开放平台下唯一）',
    access_token   VARCHAR(255)                                                    NOT NULL COMMENT '微信访问令牌，用于调用微信API',
    refresh_token  VARCHAR(255)                                                    NULL COMMENT '微信刷新令牌，用于刷新access_token',
    expires_in     INT                                                             NOT NULL COMMENT 'access_token有效时长（秒）',
    scope          VARCHAR(100)                                                    NULL COMMENT '授权作用域，如snsapi_userinfo',
    create_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP                             NOT NULL COMMENT '记录创建时间',
    update_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '记录更新时间',

    -- 索引
    INDEX idx_auth_id (user_id) COMMENT 'user_id的索引，加速按auth_id查询',
    INDEX idx_openid (openid) COMMENT '微信OpenID索引，加速按OpenID查询',
    INDEX idx_unionid (unionid) COMMENT '微信UnionID索引，加速按UnionID查询'
)
    COMMENT = '微信授权信息表，存储通过微信授权登录的用户信息和授权凭证';

-- QQ授权信息表，用于存储QQ授权登录信息
CREATE TABLE IF NOT EXISTS qq_auth
(
    qq_auth_id    CHAR(36)                                                        NOT NULL COMMENT '记录ID，由UUID生成'
        PRIMARY KEY,
    user_id       CHAR(36)                                                        NOT NULL COMMENT '关联auth表的ID',
    openid        VARCHAR(50)                                                     NOT NULL UNIQUE COMMENT 'QQ OpenID，唯一标识用户在应用中的身份',
    access_token  VARCHAR(255)                                                    NOT NULL COMMENT 'QQ Access Token，用于调用QQ OpenAPI',
    refresh_token VARCHAR(255)                                                    NULL COMMENT 'QQ Refresh Token，用于刷新Access Token',
    expires_in    INT                                                             NOT NULL COMMENT 'access_token的有效期，单位为秒',
    scope         VARCHAR(255)                                                    NULL COMMENT '授权作用域，用户授权的权限范围',
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP                             NOT NULL COMMENT '记录创建时间',
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '记录更新时间',

    -- 索引
    INDEX idx_auth_id (user_id) COMMENT 'user_id的索引，加速按user_id查询',
    INDEX idx_openid (openid) COMMENT 'QQ OpenID的索引，加速按OpenID查询'
)
    COMMENT = 'QQ授权信息表，存储通过QQ授权登录的用户信息和授权凭证';

-- 用户角色表，用于存储用户与角色的关联信息
CREATE TABLE IF NOT EXISTS user_role
(
    user_role_id CHAR(36)                                                        NOT NULL COMMENT '用户角色关系ID，由UUID生成'
        PRIMARY KEY,
    user_id      CHAR(36)                                                        NOT NULL COMMENT '关联auth表的ID',
    role_id      CHAR(36)                                                        NOT NULL COMMENT '关联role表的ID',
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP                             NOT NULL COMMENT '关系创建时间',
    update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '记录更新时间',

    -- 索引
    INDEX idx_auth_id (user_id) COMMENT '用户ID索引，加速按用户ID查询',
    INDEX idx_role_id (role_id) COMMENT '角色ID索引，加速按角色ID查询'
)
    COMMENT = '用户角色表，存储用户与角色的关联信息';

-- 角色权限表，用于存储角色与权限的关联信息
CREATE TABLE IF NOT EXISTS role_permission
(
    role_permission_id CHAR(36)                                                        NOT NULL COMMENT '角色权限关系ID，由UUID生成'
        PRIMARY KEY,
    role_id            CHAR(36)                                                        NOT NULL COMMENT '关联role表的ID',
    permission_id      CHAR(36)                                                        NOT NULL COMMENT '关联permission表的ID',
    create_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP                             NOT NULL COMMENT '关系创建时间',
    update_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '记录更新时间',

    -- 索引
    INDEX idx_role_id (role_id) COMMENT '角色ID索引，加速按角色ID查询',
    INDEX idx_permission_id (permission_id) COMMENT '权限ID索引，加速按权限ID查询'
)
    COMMENT = '角色权限表，存储角色与权限的关联信息';

-- 权限表，用于定义系统中的各项权限
CREATE TABLE IF NOT EXISTS permission
(
    permission_id   VARCHAR(36)                         NOT NULL COMMENT '权限ID，由UUID生成'
        PRIMARY KEY,
    permission_name VARCHAR(50) UNIQUE                  NOT NULL COMMENT '权限名称，如查看用户、编辑用户、删除用户',
    description     VARCHAR(255)                        NULL COMMENT '权限描述，便于说明权限的作用',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '权限创建时间',

    -- 索引
    INDEX idx_permission_name (permission_name) COMMENT '权限名称索引，加速按权限名称查询'
)
    COMMENT = '权限表，定义系统中的各项权限';
