-- internal member module permissions
-- use osh_permission / osh_role_permission

insert into `osh_permission`
(`permission_name`, `permission_code`, `description`, `parent_id`, `type`, `url`, `path`, `component`, `sort_order`, `create_by`, `update_by`, `delete_flag`)
select
    'Internal Member Module',
    'internal:member',
    'Internal member module entry',
    0,
    1,
    null,
    '/internal-member',
    null,
    210,
    'system',
    'system',
    0
from dual
where not exists (
    select 1 from `osh_permission` p where p.`permission_code` = 'internal:member' and p.`delete_flag` = 0
);

insert into `osh_permission`
(`permission_name`, `permission_code`, `description`, `parent_id`, `type`, `url`, `path`, `component`, `sort_order`, `create_by`, `update_by`, `delete_flag`)
select
    'Paid Member Overview',
    'internal:member:list',
    'View paid member overview and outcome list',
    p.id,
    2,
    '/pc/admin/internal-member/page',
    null,
    null,
    1,
    'system',
    'system',
    0
from `osh_permission` p
where p.`permission_code` = 'internal:member'
  and p.`delete_flag` = 0
  and not exists (
      select 1 from `osh_permission` c where c.`permission_code` = 'internal:member:list' and c.`delete_flag` = 0
  );

insert into `osh_permission`
(`permission_name`, `permission_code`, `description`, `parent_id`, `type`, `url`, `path`, `component`, `sort_order`, `create_by`, `update_by`, `delete_flag`)
select
    'Paid Member Detail',
    'internal:member:detail',
    'View paid member study detail, employment profile and follow records',
    p.id,
    2,
    '/pc/admin/internal-member/{userId}/summary',
    null,
    null,
    2,
    'system',
    'system',
    0
from `osh_permission` p
where p.`permission_code` = 'internal:member'
  and p.`delete_flag` = 0
  and not exists (
      select 1 from `osh_permission` c where c.`permission_code` = 'internal:member:detail' and c.`delete_flag` = 0
  );

insert ignore into `osh_role_permission` (`role_id`, `permission_id`, `create_by`, `update_by`, `delete_flag`)
select r.id, p.id, 'system', 'system', 0
from `osh_role` r
join `osh_permission` p on p.`permission_code` in ('internal:member', 'internal:member:list', 'internal:member:detail')
where r.`delete_flag` = 0
  and r.`status` = 1
  and r.`role_code` in ('manager', 'core_developer', 'founder');
