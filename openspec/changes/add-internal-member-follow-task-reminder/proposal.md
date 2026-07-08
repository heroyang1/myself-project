# Proposal: add-internal-member-follow-task-reminder

## Background

The current internal-member follow-record capability only stores historical follow-up entries that have already happened.
It does not model future follow-up obligations such as:

- a planned follow-up time
- a responsible admin who must act in the future
- reminder delivery before the due time
- task status such as pending, completed, canceled, or overdue

Trying to keep adding these concepts into `follow_record` would mix past-tense history with future-tense task management.

## Goals

- Add a dedicated internal-member follow-task capability for future follow-up work.
- Support task status management without changing existing follow-record semantics.
- Send reminder email to the responsible admin before the planned follow-up time.
- Allow admins to create, edit, cancel, complete, and list follow tasks.
- Optionally append a historical follow-record when a follow task is completed.

## Non-Goals

- No workflow engine or BPM platform integration.
- No SMS / enterprise IM / push-channel support in v1.
- No configurable multi-step approval flow.
- No frontend redesign outside the follow-task feature scope.

## User Impact

- Admins can manage internal-member follow-up as tasks instead of only free-form historical notes.
- Responsible admins receive reminder email before the planned follow-up time.
- Task status becomes visible and operable in the admin UI.
- Existing follow-record read/write APIs remain available for historical logs.

## Risks

- Reminder email failure must not block task creation or status updates.
- Existing internal-member admin user scope must stay aligned when assigning responsible admins.
- Scheduled reminder scanning must avoid duplicate repeated reminders for the same task.

## Acceptance Criteria

- [ ] New follow-task create/list/detail/update/status/complete APIs are available for internal-member admin usage.
- [ ] Follow tasks keep a clear status lifecycle independent from historical follow-records.
- [ ] Reminder email is sent when a pending task reaches its reminder time and the responsible admin has an email address.
- [ ] A task becomes overdue when the planned follow-up time passes without completion or cancelation.
- [ ] Completing a follow task can append a historical follow-record entry while preserving operator vs responsible-admin semantics.
