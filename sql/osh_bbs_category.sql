CREATE TABLE `osh_bbs_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(100) NOT NULL COMMENT '分类标题',
  `image` varchar(255) DEFAULT NULL COMMENT '分类图片/图标',
  `sort` int(11) DEFAULT '0' COMMENT '排序',
  `is_delete` tinyint(1) DEFAULT '0' COMMENT '是否删除 0否 1是',
  `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-删除（拦截器引用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区分类表';