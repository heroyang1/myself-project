-- 工具展开背景图字段
ALTER TABLE `osh_tool`
  ADD COLUMN `background_image` varchar(500) DEFAULT NULL COMMENT '工具展开背景图OSS相对路径' AFTER `github_url`;
