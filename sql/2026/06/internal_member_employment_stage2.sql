alter table `osh_internal_member_employment_profile`
    add column `resume_delivery_count` int(11) default null comment 'Resume delivery count' after `remark`,
    add column `interview_company_count` int(11) default null comment 'Interview company count' after `resume_delivery_count`,
    add column `pending_interview_count` int(11) default null comment 'Pending interview company count' after `interview_company_count`,
    add column `completed_interview_count` int(11) default null comment 'Completed interview company count' after `pending_interview_count`,
    add column `interview_round_count` int(11) default null comment 'Interview round count' after `completed_interview_count`,
    add column `interview_status` varchar(64) default null comment 'Interview status' after `interview_round_count`,
    add column `interview_result` varchar(64) default null comment 'Interview result' after `interview_status`,
    add column `last_interview_time` datetime default null comment 'Last interview time' after `interview_result`;
