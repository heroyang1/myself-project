# Design: add-internal-member-follow-record-responsible-user

## Overview

Extend the internal member follow-record capability with a dedicated responsible-admin field.

- `operatorId` / `operatorName`: who created the record
- `responsibleUserId` / `responsibleUserName`: who is currently assigned to follow up

## API Contract

### Create follow record

- Method: `POST`
- Path: `/pc/admin/internal-member/{userId}/follow-records`
- Request body adds:
  - `responsibleUserId` required `Long`

### List follow records

- Method: `GET`
- Path: `/pc/admin/internal-member/{userId}/follow-records`
- Each row adds:
  - `responsibleUserId`
  - `responsibleUserName`

## Validation

- `responsibleUserId` is required when creating a follow record.
- `responsibleUserId` must belong to the internal-member admin role set:
  - `manager`
  - `core_developer`
  - `founder`

## Persistence

Add nullable column:

- table: `osh_internal_member_follow_record`
- column: `responsible_user_id bigint`

Add index:

- `idx_internal_member_follow_record_responsible_user_id (responsible_user_id)`

## Query Changes

For follow-record list queries:

- select `r.responsible_user_id as responsibleUserId`
- left join `osh_user ru on ru.id = r.responsible_user_id`
- select `ru.username as responsibleUserName`

## Compatibility

- Existing follow-record rows remain readable.
- Existing admin-user selection API remains unchanged and is reused by frontend for responsible-admin options.
