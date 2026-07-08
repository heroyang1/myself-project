# Proposal: add-internal-member-follow-record-responsible-user

## Background

The internal member follow-record APIs currently store only `operatorId`, which represents the admin user who created the follow record. The small-class user detail page now needs a separate assignment field for the current follow owner.

Reusing `operatorId` would collapse two different meanings:

- who created or operated on the record
- who is responsible for following up

The backend needs to expose these as separate fields.

## Goals

- Add `responsibleUserId` to the follow-record create API.
- Return `responsibleUserId` and `responsibleUserName` from the follow-record list API.
- Preserve `operatorId` / `operatorName` semantics as the actual creator/operator.
- Restrict `responsibleUserId` to the internal-member admin scope.

## Non-Goals

- No change to the admin-user dropdown API.
- No change to other internal-member APIs.
- No change to follow-record update behavior; this change only covers append/create and list read.

## User Impact

- Frontend can assign a responsible admin when appending a follow record.
- Follow-record lists can display both the operator and the responsible admin.

## Risks

- The responsible admin scope must stay aligned with internal-member module admin roles.
- Existing rows may have `responsible_user_id` as `null` until data is backfilled or new records are created.

## Acceptance Criteria

- [ ] `POST /pc/admin/internal-member/{userId}/follow-records` accepts `responsibleUserId`.
- [ ] `GET /pc/admin/internal-member/{userId}/follow-records` returns `responsibleUserId` and `responsibleUserName`.
- [ ] `operatorId` continues to represent the actual creator/operator.
- [ ] `responsibleUserId` must belong to the internal-member admin role set.
