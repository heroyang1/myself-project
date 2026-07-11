# Spec: internal-member-follow-task

## ADDED Requirements

### Requirement: Internal-member admins can manage follow tasks separately from follow records

The system SHALL provide a dedicated follow-task capability for internal-member follow-up work that is planned for the future or currently in progress.

#### Scenario: Create follow task

- GIVEN a paid internal-member user exists
- AND a valid internal-member admin is selected as the responsible user
- WHEN a client calls `POST /pc/admin/internal-member/{userId}/follow-tasks`
- THEN the system creates a follow task in `pending` status

#### Scenario: List follow tasks

- GIVEN follow tasks exist for a paid internal-member user
- WHEN a client calls `GET /pc/admin/internal-member/{userId}/follow-tasks`
- THEN the system returns the task list including status, planned follow time, remind time, responsible admin, and notification fields

### Requirement: Follow-task status has a bounded lifecycle

The system SHALL keep follow-task status in a fixed bounded lifecycle rather than using free-form status text.

#### Scenario: Complete follow task

- GIVEN a follow task is currently `pending`, `notified`, or `overdue`
- WHEN a client completes the task
- THEN the system marks the task as `completed`
- AND stores completion operator and completion time

#### Scenario: Cancel follow task

- GIVEN a follow task is currently `pending`, `notified`, or `overdue`
- WHEN a client updates the task status to canceled
- THEN the system marks the task as `canceled`

#### Scenario: Overdue follow task

- GIVEN a follow task is currently `pending` or `notified`
- AND the planned follow time has passed
- WHEN the overdue scan runs
- THEN the system marks the task as `overdue`

### Requirement: Reminder email is sent before planned follow-up time

The system SHALL support reminder email delivery for follow tasks.

#### Scenario: Send reminder email

- GIVEN a follow task is `pending`
- AND `notify_sent = 0`
- AND `remind_time <= now`
- AND the responsible admin user has a non-empty email
- WHEN the reminder scan runs
- THEN the system sends reminder email to that responsible admin
- AND updates the task notification fields
- AND marks the task as `notified`

#### Scenario: Responsible admin has no email

- GIVEN a follow task is due for reminder
- AND the responsible admin has no email
- WHEN the reminder scan runs
- THEN the system skips email sending
- AND does not fail the scheduler run

#### Scenario: Reminder delivery failure

- GIVEN a follow task is due for reminder
- AND the mail infrastructure throws an error
- WHEN the reminder scan runs
- THEN the system records the failure in logs
- AND does not stop processing other tasks

### Requirement: Completing a follow task preserves historical record semantics

The system SHALL preserve follow-record semantics as historical logs of actions that already happened.

#### Scenario: Completion appends follow record

- GIVEN a follow task is completed
- WHEN the completion request succeeds
- THEN the system appends a historical follow-record entry for the same internal-member user
- AND `operatorId` continues to mean the actual completion operator
- AND `responsibleUserId` continues to mean the assigned follow-up admin
