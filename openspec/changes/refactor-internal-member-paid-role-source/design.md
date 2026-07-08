# Design: refactor-internal-member-paid-role-source

## Overview

This refactor replaces `osh_member_order` as the paid-member source inside the `internalmember` admin module.
The new source is the current active member role set from `osh_user_role + osh_role`.

The implementation stays mapper-centric:

- keep controller signatures unchanged
- keep service method signatures unchanged
- keep VO field names unchanged
- replace SQL member-source subqueries and paid-member validation SQL only

## Current Hotspots

Relevant code paths:

- controller: `InternalMemberAdminController`
- services:
  - `InternalMemberStudyServiceImpl`
  - `InternalMemberServiceImpl`
- mappers:
  - `InternalMemberStudyMapper.xml`
  - `InternalMemberMapper.xml`

Historical issues before this change:

- `countPaidMemberByUserId` depended on `osh_member_order`
- `selectMemberStudySummary` depended on the latest paid member order
- `selectPaidMemberStudyOverview` depended on the latest paid member order
- `selectOutcomeList` depended on the latest paid member order

## Source Rule

The canonical active paid-member set is:

- role code in `('vip', 'small_class')`
- role record is not deleted
- role is enabled
- user role record is not deleted
- `expire_time` is not null
- `expire_time > now()`

When a user has multiple active member roles, the SQL keeps this priority:

1. `small_class`
2. `vip`
3. later `expire_time`
4. later update/create order as tie-breaker

## Implemented SQL Scope

Completed offline implementation:

- `InternalMemberMapper.xml -> countPaidMemberByUserId`
- `InternalMemberStudyMapper.xml -> countPaidMemberByUserId`
- `InternalMemberStudyMapper.xml -> selectMemberStudySummary`
- `InternalMemberStudyMapper.xml -> selectPaidMemberStudyOverview`
- `InternalMemberMapper.xml -> selectOutcomeList`
- duplicated fallback queries in `InternalMemberMapper.xml -> selectPaidMemberStudyOverview`
- duplicated fallback queries in `InternalMemberMapper.xml -> selectMemberStudySummary`

## Compatibility

Preserved:

- endpoint paths
- request parameters
- VO field names
- response envelopes

Changed semantics:

- `planName` is still returned but now represents current member identity display rather than order snapshot
- list and detail membership visibility now depend on active role state instead of historical orders

## Validation Plan

Runtime validation should cover:

- `GET /pc/admin/internal-member/{userId}/summary`
- `GET /pc/admin/internal-member/page`
- `GET /pc/admin/internal-member/outcome/page`
- at least one guarded detail endpoint such as `courses` or `study-attitude`

Expected outcomes:

- users with only historical orders are rejected or absent
- users with active `vip` or `small_class` roles remain accessible
- response fields remain unchanged

Detailed steps are documented in:

- `docs/internal-member-admin-api/21-role-source-acceptance-checklist.md`

## Rollback

Rollback is SQL-only for this refactor:

- restore previous member-source subqueries in `InternalMemberStudyMapper.xml`
- restore previous member-source subqueries in `InternalMemberMapper.xml`
