# Design: add-internal-member-follow-task-reminder

## Overview

This change adds a dedicated follow-task model to the internal-member admin module.

- `follow_record`: historical log of follow-up actions that already happened
- `follow_task`: future or ongoing follow-up obligation that can be reminded, completed, canceled, or become overdue

The implementation stays within the existing internal-member backend and reuses:

- internal-member admin user scope validation
- `osh_user.email` as the reminder destination
- existing mail infrastructure via `EmailUtil`
- existing scheduler support via Spring `@Scheduled`

## API Contract

### New APIs

| Method | Path | Purpose |
|---|---|---|
| GET | `/pc/admin/internal-member/{userId}/follow-tasks` | List follow tasks for a paid member |
| POST | `/pc/admin/internal-member/{userId}/follow-tasks` | Create a follow task |
| PUT | `/pc/admin/internal-member/follow-tasks/{taskId}` | Edit a follow task |
| PUT | `/pc/admin/internal-member/follow-tasks/{taskId}/status` | Update task status |
| POST | `/pc/admin/internal-member/follow-tasks/{taskId}/complete` | Complete task with optional completion note |

### Existing APIs kept unchanged

- `GET /pc/admin/internal-member/{userId}/follow-records`
- `POST /pc/admin/internal-member/{userId}/follow-records`

They remain the historical follow-record capability.

## Data Design

Add table: `osh_internal_member_follow_task`

Suggested fields:

- `id bigint`
- `user_id bigint`
- `content varchar(2000)`
- `responsible_user_id bigint`
- `planned_follow_time datetime`
- `remind_time datetime`
- `status varchar(32)`
- `notify_channel varchar(32)`
- `notify_sent tinyint`
- `notify_sent_time datetime`
- `completed_time datetime`
- `completed_by bigint`
- `result_note varchar(2000)`
- `create_by bigint`
- `create_time datetime`
- `update_by bigint`
- `update_time datetime`
- `delete_flag tinyint`

Suggested indexes:

- `idx_internal_member_follow_task_user_id (user_id)`
- `idx_internal_member_follow_task_responsible_user_id (responsible_user_id)`
- `idx_internal_member_follow_task_remind_scan (status, notify_sent, remind_time)`
- `idx_internal_member_follow_task_planned_scan (status, planned_follow_time)`

## Status Model

V1 uses a small fixed state set:

- `pending`
- `notified`
- `completed`
- `canceled`
- `overdue`

### State rules

- create -> `pending`
- reminder sent -> `notified`
- pending/notified -> `completed`
- pending/notified/overdue -> `canceled`
- pending/notified -> `overdue` when planned time passes

## Validation

- target `userId` must still pass internal-member paid-member validation
- `responsibleUserId` is required
- `responsibleUserId` must belong to the internal-member admin role set
- `content` is required and trimmed
- `plannedFollowTime` is required
- `remindTime` is required in v1
- `remindTime` must be earlier than or equal to `plannedFollowTime`

## Reminder Delivery

Reminder channel in v1:

- `email`

Reminder job behavior:

1. scan tasks where:
   - `delete_flag = 0`
   - `status in ('pending', 'notified')`
   - `notify_sent = 0`
   - `remind_time <= now()`
2. load responsible admin user email
3. if email is empty:
   - skip sending
   - keep task status unchanged
   - log info
4. if email exists:
   - send reminder email
   - on success:
     - set `notify_sent = 1`
     - set `notify_sent_time = now()`
     - if current status is `pending`, update to `notified`
   - on failure:
     - log warn/error
     - do not block scheduler loop

## Overdue Scan

Separate scheduled scan:

- find tasks where:
  - `status in ('pending', 'notified')`
  - `planned_follow_time < now()`
- set status to `overdue`

## Completion Behavior

Completing a task:

- updates status to `completed`
- stores `completed_time`, `completed_by`, and optional `result_note`
- appends a new historical follow-record entry derived from the task completion result

This keeps historical follow-record semantics intact while letting tasks drive the future workflow.

## Package Scope

Expected backend changes mainly in:

- controller: `InternalMemberAdminController`
- service: `IInternalMemberService`, `InternalMemberServiceImpl`
- mapper: `InternalMemberMapper`, `InternalMemberMapper.xml`
- DTO/VO: internalmember dto/vo package
- scheduled job: new internal-member follow-task reminder task
- SQL incremental script under `sql/2026/07/`

## Compatibility

- Existing follow-record APIs remain compatible.
- Existing follow-record rows and semantics remain unchanged.
- New APIs are additive.
- Email notification is a side effect and must not break main task operations.

## Validation Plan

- controller/service unit tests for create, update, status, and complete flows
- paid-member validation on list/create
- responsible-admin validation
- reminder scan marks email-sent tasks as notified
- overdue scan marks expired tasks as overdue
- runtime smoke verification through local APIs in a running environment
