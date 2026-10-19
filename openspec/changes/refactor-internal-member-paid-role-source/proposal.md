# Proposal: refactor-internal-member-paid-role-source

## Background

The `internalmember` admin module historically treated a "paid member" as a user who had a paid row in `osh_member_order`.
That no longer matches the current business truth. The current valid paid-member identity should come from active member roles in:

- `osh_user_role`
- `osh_role`

The old order-based source causes three visible problems:

- list APIs can include users who only bought membership historically but no longer hold an active member role
- detail and operation APIs can drift from list APIs if different mapper SQL keeps using different sources
- member display fields such as `planName` and `memberExpireTime` become tied to order snapshots instead of current member identity

## Goal

- unify paid-member source logic in the `internalmember` module on active `vip` / `small_class` roles
- keep endpoint paths, request parameters, VO fields, and response shape unchanged
- finish the mapper-side source replacement first, then leave runtime validation for a reachable environment

## Non-Goals

- no API contract change
- no database schema change
- no change to other unrelated member-order business flows
- no new frontend fields

## User Impact

- all `internalmember` endpoints except `GET /pc/admin/internal-member/admin-users` are affected by the member-source rule
- users with historical member orders but no current active `vip` / `small_class` role will no longer be treated as paid members
- `planName` remains present, but its meaning shifts from order snapshot to current member identity display

## Risks

- if role data is stale or dirty, some users previously visible through historical orders may disappear
- `planName` semantic change may affect operator expectations even though the response field remains unchanged
- rollback must stay easy and SQL-scoped because this change is implemented at mapper level

## Acceptance Criteria

- [ ] `countPaidMemberByUserId` uses active-role source in both mappers
- [ ] `GET /pc/admin/internal-member/{userId}/summary` sources member fields from active roles without changing VO fields
- [ ] `GET /pc/admin/internal-member/page` builds the member set from active roles
- [ ] `GET /pc/admin/internal-member/outcome/page` builds the member set from active roles
- [ ] runtime smoke validation passes in a reachable environment with the prepared token

## Related Documents

- implementation plan: `docs/internal-member-admin-api/19-paid-member-role-refactor-plan.md`
- progress log: `docs/internal-member-admin-api/20-refactor-progress-log.md`
- acceptance checklist: `docs/internal-member-admin-api/21-role-source-acceptance-checklist.md`
