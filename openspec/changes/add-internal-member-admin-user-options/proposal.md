# Proposal: add-internal-member-admin-user-options

## Background

The internal member admin module currently supports outcome pages, employment profiles, and follow records, but it does not expose a dedicated API for selecting admin users from the `osh_user` domain.

Frontend follow-up workflows need a lightweight dropdown-style list of admin users for assignment and filtering. Reusing the existing full user management API is not appropriate because that API is broader in scope and is not tailored to internal member admin usage.

## Goals

- Add a dedicated admin-user query API under the internal member admin module.
- Return only admin users, not ordinary users.
- Keep the response lightweight so frontend dropdowns can consume it directly.
- Support optional keyword filtering for username/email lookup.

## Non-Goals

- No pagination is added in this change.
- No changes to follow record persistence are included in this change.
- No changes to ordinary user query APIs are included in this change.
- No new database tables or columns are introduced.

## User Impact

- Frontend can fetch a dedicated admin-user option list from the internal member module.
- The response is limited to users with admin roles in the `osh_user` / `osh_role` domain.

## Risks

- Role filtering must stay aligned with the internal member permission model.
- If a user has multiple admin roles, the API should return the highest active admin role consistently.

## Acceptance Criteria

- [ ] Add `GET /pc/admin/internal-member/admin-users`.
- [ ] The API returns only users whose active roles are in the admin role set for the internal member module.
- [ ] The API supports optional `keyword` filtering.
- [ ] The API does not return ordinary users.
