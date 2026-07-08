# Tasks: add-internal-member-follow-task-reminder

## Preparation

- [x] Review current internal-member follow-record flow and responsible-admin model
- [x] Confirm existing mail and scheduler infrastructure can be reused

## Implementation

- [x] Add OpenSpec proposal, design, tasks, and spec
- [x] Add follow-task DTOs and VOs
- [x] Add follow-task mapper methods and SQL
- [x] Add follow-task service methods and validation
- [x] Expose follow-task APIs in `InternalMemberAdminController`
- [x] Add scheduled reminder and overdue scan task
- [x] Append a historical follow-record when a task is completed
- [x] Add database incremental SQL for `osh_internal_member_follow_task`

## Verification

- [x] Add or update controller tests
- [x] Add or update service tests
- [x] Run targeted compile/tests
- [ ] Smoke verify follow-task APIs in local runtime

## PR

- [ ] Link PR to OpenSpec change
- [ ] Record exact reminder and status verification results

Verification notes:

- `mvn -pl backstage-system -am test "-Dtest=InternalMemberServiceImplTest,InternalMemberAdminControllerTest" -DfailIfNoTests=false "-Dsurefire.failIfNoSpecifiedTests=false"` completed successfully, but Surefire executed `0` tests in this environment while test classes compiled successfully.
- `mvn -pl backstage-admin -am -DskipTests compile` completed successfully.
- Local runtime smoke verification is currently blocked because the existing 8081 process was not running, and `spring-boot:run` could not be used directly under the current machine constraints:
  - default Maven repo path is not writable for plugin metadata updates
  - switching to a workspace-local Maven repo then hit outbound dependency resolution permission denial
