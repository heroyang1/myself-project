alter table osh_internal_member_employment_profile
add column resume_file_key varchar(512) null comment '脱敏后简历PDF云端Key' after remark,
add column resume_file_name varchar(255) null comment '脱敏后简历文件名' after resume_file_key,
add column offer_file_key varchar(512) null comment '脱敏后offer PDF云端Key' after resume_file_name,
add column offer_file_name varchar(255) null comment '脱敏后offer文件名' after offer_file_key;
