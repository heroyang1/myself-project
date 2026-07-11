# Tasks: add-internal-member-follow-record-responsible-user

## Preparation

- [x] Review current internal-member follow-record API and mapper flow
- [x] Confirm admin scope alignment with internal-member permissions

## Implementation

- [ ] Add OpenSpec proposal, design, tasks, and spec
- [ ] Extend follow-record create DTO and list VO with responsible-admin fields
- [ ] Validate responsible admin in service layer
- [ ] Persist `responsible_user_id` in mapper SQL
- [ ] Return responsible admin fields in follow-record list query
- [ ] Add database incremental SQL

## Verification

- [ ] Add or update controller tests
- [ ] Add or update service tests
- [ ] Run targeted verification
