# Tasks: refactor-internal-member-paid-role-source

## Preparation

- [x] Read `openspec/project.md`
- [x] Read repository AI workflow documents
- [x] Read the active change documents and internal refactor progress notes

## Implementation

- [x] Change `InternalMemberMapper.xml -> countPaidMemberByUserId` to role-based source
- [x] Change `InternalMemberStudyMapper.xml -> countPaidMemberByUserId` to role-based source
- [x] Change `InternalMemberStudyMapper.xml -> selectMemberStudySummary` to source member info from active roles
- [x] Change `InternalMemberStudyMapper.xml -> selectPaidMemberStudyOverview` to source member set and member fields from active roles
- [x] Change `InternalMemberMapper.xml -> selectOutcomeList` to source member set from active roles
- [x] Align duplicated fallback queries in `InternalMemberMapper.xml -> selectPaidMemberStudyOverview` and `selectMemberStudySummary` to the same active-role source

## Verification

- [ ] Run targeted verification successfully in a compilable repository state
- [x] Smoke test `GET /pc/admin/internal-member/{userId}/summary` with test token
- [x] Smoke test `GET /pc/admin/internal-member/page` with test token
- [x] Smoke test `GET /pc/admin/internal-member/outcome/page` with test token

Verification notes:

- Local startup was recovered after deleting stale `target` directories and recompiling with `mvn -pl backstage-admin -am -DskipTests compile`
- The previous `OutboxEventPublisher.class` failure came from incomplete local build artifacts, not from this role-source refactor
- Runtime smoke verification on `127.0.0.1:8081` succeeded for `summary`, `page`, and `outcome/page`
- Additional local smoke verification also succeeded for the remaining internal-member read/write endpoints
- In this environment the provided JWT had to be sent via request header `token`; `Authorization: Bearer ...` returned `401`
- Tomorrow's exact acceptance steps are documented in `docs/internal-member-admin-api/21-role-source-acceptance-checklist.md`
- The implementation should be understood as "few core source changes affecting the whole 17-endpoint scope", not "17 separate endpoint rewrites"
- Shared paid-member validation now covers most single-user detail and operation endpoints automatically
- Dedicated SQL rewrites were only needed for queries that directly sourced member identity from the old order tables

## PR

- [ ] Link PR to OpenSpec change
- [ ] Fill in final runtime verification results
- [ ] Fill in risks and rollback notes using the acceptance checklist and progress log
