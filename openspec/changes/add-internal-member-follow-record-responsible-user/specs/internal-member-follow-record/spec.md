# Spec: internal-member-follow-record

## MODIFIED Requirements

### Requirement: Internal member follow records shall separate operator and responsible admin

The system SHALL preserve the creator/operator identity separately from the responsible follow-up admin.

#### Scenario: Create a follow record with responsible admin

- GIVEN a client calls `POST /pc/admin/internal-member/{userId}/follow-records`
- WHEN the request includes a valid `responsibleUserId`
- THEN the system SHALL save `operatorId` as the current logged-in admin
- AND the system SHALL save `responsibleUserId` as the assigned follow-up admin

#### Scenario: Responsible admin is missing

- GIVEN a client calls `POST /pc/admin/internal-member/{userId}/follow-records`
- WHEN `responsibleUserId` is empty
- THEN the system SHALL reject the request

#### Scenario: Responsible admin is outside internal-member admin scope

- GIVEN a client calls `POST /pc/admin/internal-member/{userId}/follow-records`
- WHEN `responsibleUserId` does not have an active admin role in `manager`, `core_developer`, or `founder`
- THEN the system SHALL reject the request

### Requirement: Follow-record list shall return responsible admin fields

The system SHALL include responsible-admin fields in internal member follow-record list responses.

#### Scenario: Return follow-record list

- GIVEN a client calls `GET /pc/admin/internal-member/{userId}/follow-records`
- WHEN follow records exist
- THEN each record SHALL include `responsibleUserId`
- AND each record SHALL include `responsibleUserName`
- AND `operatorId` / `operatorName` SHALL continue to indicate who created the record
