# Design: add-internal-member-admin-user-options

## Overview

Add a lightweight read-only API to `InternalMemberAdminController`:

- `GET /pc/admin/internal-member/admin-users`

The API will query the `osh_user` domain and return selectable admin users for the internal member module.

## Role Scope

This module already assigns internal member permissions to:

- `manager`
- `core_developer`
- `founder`

This same role set will define which users are eligible to appear in the new API.

## API Contract

### Request

- Method: `GET`
- Path: `/pc/admin/internal-member/admin-users`
- Query params:
  - `keyword` optional string

### Response

Return `R<List<InternalMemberAdminUserOptionVO>>`.

Each item contains:

- `userId`
- `username`
- `email`
- `avatar`
- `roleCode`
- `roleName`
- `roleLevel`

## Query Rules

- Read from `osh_user`, `osh_user_role`, and `osh_role`.
- Only include active user-role bindings:
  - `osh_user_role.delete_flag = 0`
  - `expire_time is null or expire_time > now()`
- Only include active roles:
  - `osh_role.delete_flag = 0`
  - `osh_role.status = 1`
- Only include admin roles:
  - `role_code in ('manager', 'core_developer', 'founder')`
- Only include normal users:
  - `osh_user.delete_flag = 0`
  - `osh_user.status = 0`
- If a user has multiple admin roles, return the highest active admin role.
- If `keyword` is present, filter by username, email, role name, or role code.

## Implementation Plan

- Add `InternalMemberAdminUserOptionVO`.
- Add service method on `IInternalMemberService`.
- Add mapper method on `InternalMemberMapper`.
- Add SQL in `InternalMemberMapper.xml`.
- Add controller endpoint in `InternalMemberAdminController`.
- Add controller and service tests.

## Compatibility

- This change adds a new API only.
- Existing internal member APIs remain unchanged.
