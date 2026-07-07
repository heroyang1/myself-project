create table if not exists osh_internal_member_growth_story (
    id bigint not null auto_increment comment '主键ID',
    user_id bigint not null comment '付费用户ID',
    story_title varchar(255) not null comment '成长故事标题',
    story_summary text null comment '成长故事摘要',
    story_content longtext not null comment '成长故事正文',
    facts_json longtext null comment '生成使用的结构化事实快照',
    generate_status varchar(32) not null default 'success' comment '生成状态',
    is_public tinyint(1) not null default 0 comment '是否公开：0-否 1-是',
    published_time datetime null comment '公开发布时间',
    create_by bigint not null comment '创建人',
    create_time datetime not null default current_timestamp comment '创建时间',
    update_by bigint not null comment '更新人',
    update_time datetime not null default current_timestamp on update current_timestamp comment '更新时间',
    delete_flag tinyint(1) not null default 0 comment '删除标记',
    primary key (id),
    unique key uk_internal_member_growth_story_user_id (user_id),
    key idx_internal_member_growth_story_public_time (is_public, published_time)
) engine=InnoDB default charset=utf8mb4 comment='付费用户成长故事表';
