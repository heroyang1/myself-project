-- 1. 帖子主表
CREATE TABLE `osh_bbs_post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '作者ID',
  `title` varchar(200) DEFAULT NULL COMMENT '标题',
  `content` longtext COMMENT '正文内容',
  `view_count` int DEFAULT '0' COMMENT '浏览量',
  `support_count` int DEFAULT '0' COMMENT '点赞数',
  `comment_count` int DEFAULT '0' COMMENT '评论数',
  `is_top` tinyint(1) DEFAULT '0' COMMENT '是否置顶',
  `is_delete` tinyint(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0-未删 1-已删，Mapper引用）',
  `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-删除（拦截器引用）',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_delete_flag` (`delete_flag`)
);