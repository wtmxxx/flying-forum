create table conversation
(
    id          bigint                             not null comment '消息ID，由MP雪花算法生成'
        primary key,
    user_id     bigint                             not null comment '用户ID',
    title       varchar(255)                       null comment '对话标题',
    create_time datetime default CURRENT_TIMESTAMP not null comment '消息创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '消息更新时间'
)
    comment '聊天消息表，存储用户和GPT的对话记录';

create index idx_user_id
    on conversation (user_id);


create table message
(
    id              bigint                                                          not null primary key, -- 使用雪花算法生成ID
    conversation_id bigint                                                          not null,
    role            varchar(50)                                                     not null,
    content         text                                                            not null,
    create_time     timestamp default current_timestamp                             not null,
    update_time     timestamp default current_timestamp on update current_timestamp not null,
    index idx_conversation_id (conversation_id)
);


