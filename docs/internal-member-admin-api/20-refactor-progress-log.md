# Internal Member Refactor Progress Log

## 2026-07-08

Current status: `Implementation complete, runtime verification pending`

Completed:

- Added OpenSpec change `refactor-internal-member-paid-role-source`
- Added implementation plan document `19-paid-member-role-refactor-plan.md`
- Switched `InternalMemberMapper.xml -> countPaidMemberByUserId` from `osh_member_order` to `osh_user_role + osh_role`
- Switched `InternalMemberStudyMapper.xml -> countPaidMemberByUserId` from `osh_member_order` to `osh_user_role + osh_role`
- Switched `InternalMemberStudyMapper.xml -> selectMemberStudySummary` member source from latest member order to current active member role
- Switched `InternalMemberStudyMapper.xml -> selectPaidMemberStudyOverview` member set and member fields from latest member order to current active member role
- Switched `InternalMemberMapper.xml -> selectOutcomeList` member set from latest member order to current active member role
- Aligned duplicated fallback queries in `InternalMemberMapper.xml -> selectPaidMemberStudyOverview` and `selectMemberStudySummary` to the same active-role source
- Kept endpoint paths, request parameters, VO fields, and response structures unchanged

Changed files:

- `backstage-system/src/main/resources/mapper/internalmember/InternalMemberMapper.xml`
- `backstage-system/src/main/resources/mapper/internalmember/InternalMemberStudyMapper.xml`
- `docs/internal-member-admin-api/20-refactor-progress-log.md`
- `docs/internal-member-admin-api/21-role-source-acceptance-checklist.md`
- `openspec/changes/refactor-internal-member-paid-role-source/proposal.md`
- `openspec/changes/refactor-internal-member-paid-role-source/design.md`
- `openspec/changes/refactor-internal-member-paid-role-source/tasks.md`
- `openspec/changes/refactor-internal-member-paid-role-source/specs/internal-member-paid-role-source/spec.md`

Verification status:

- Startup issue was resolved by clearing stale module `target` directories and recompiling:
  - `mvn -pl backstage-admin -am -DskipTests compile`
- The previous `OutboxEventPublisher.class` startup failure was caused by incomplete local build output rather than the internal-member SQL refactor
- Runtime smoke verification was executed against local `http://127.0.0.1:8081`
- Important request-header detail:
  - these endpoints accepted the provided JWT through request header `token`
  - `Authorization: Bearer ...` returned `401` in this environment

Pending:

- Run the acceptance checklist in `21-role-source-acceptance-checklist.md`
- Optional follow-up:
  - run repository test classes if needed
  - capture screenshots or exported evidence if needed for PR/review

Current understanding snapshot:

- The original 17-interface impact scope is considered covered by the current implementation
- This does not mean all 17 interfaces required separate code edits
- Most impacted endpoints were switched indirectly by replacing the shared paid-member gate:
  - `InternalMemberServiceImpl.ensurePaidMember -> InternalMemberMapper.countPaidMemberByUserId`
  - `InternalMemberStudyServiceImpl.ensurePaidMember -> InternalMemberStudyMapper.countPaidMemberByUserId`
- Only the endpoints whose own SQL directly depended on the old order source required dedicated mapper changes:
  - `GET /pc/admin/internal-member/{userId}/summary`
  - `GET /pc/admin/internal-member/page`
  - `GET /pc/admin/internal-member/outcome/page`
- `GET /pc/admin/internal-member/{userId}/study-attitude` is also covered because it depends on both shared paid-member validation and `selectMemberStudySummary`
- The internalmember module runtime path no longer depends on `osh_member_order` to determine whether a user is a paid member
- Major runtime verification has now been completed locally, and remaining work is mainly documentation polish or optional extra regression coverage

Runtime verification results snapshot:

- Passed:
  - `GET /pc/admin/internal-member/admin-users`
  - `GET /pc/admin/internal-member/page`
  - `GET /pc/admin/internal-member/outcome/page`
  - `GET /pc/admin/internal-member/{userId}/summary`
  - `GET /pc/admin/internal-member/{userId}/courses`
  - `GET /pc/admin/internal-member/{userId}/study-attitude`
  - `GET /pc/admin/internal-member/{userId}/employment-profile`
  - `PUT /pc/admin/internal-member/{userId}/employment-profile`
  - `GET /pc/admin/internal-member/{userId}/follow-records`
  - `POST /pc/admin/internal-member/{userId}/follow-records`
  - `GET /pc/admin/internal-member/{userId}/exam-summary`
  - `GET /pc/admin/internal-member/{userId}/salary-summary`
  - `GET /pc/admin/internal-member/{userId}/resume-summary`
  - `GET /pc/admin/internal-member/{userId}/interview-summary`
  - `POST /pc/admin/internal-member/{userId}/documents`
  - `GET /pc/admin/internal-member/{userId}/documents/preview`
  - `GET /pc/admin/internal-member/{userId}/documents/download`
  - `DELETE /pc/admin/internal-member/{userId}/documents`
- Verified role-source behavior:
  - active `small_class` test user `2099200000000000006` passed summary/detail access
  - active `vip` test user `2099200000000000019` passed summary access
  - non-paid founder user `2049118440688398338` was rejected by `summary` with `The user is not a paid member or has no valid paid record`
- Document cleanup behavior:
  - after document deletion, preview returned `Employment document not found`, which matches expectations

Resume hint:

- Resume from: `Core runtime verification completed locally on 8081; next step is optional extra regression coverage or PR-ready summary`
