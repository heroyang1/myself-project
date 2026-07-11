# Spec: internal-member-admin-user-options

## ADDED Requirements

### Requirement: Internal member admin module shall expose a dedicated admin-user option API

The system SHALL expose a dedicated API for querying selectable admin users in the internal member admin module.

#### Scenario: Fetch admin user options

- GIVEN a client calls `GET /pc/admin/internal-member/admin-users`
- WHEN the request is processed successfully
- THEN the response SHALL return a list of admin users in `R.data`
- AND the response SHALL be suitable for frontend dropdown selection

### Requirement: The API shall return admin users only

The system SHALL exclude ordinary users from the admin-user option API.

#### Scenario: User has no admin role

- GIVEN a user only has ordinary or member roles such as `normal`, `vip`, or `small_class`
- WHEN the user query runs for `GET /pc/admin/internal-member/admin-users`
- THEN that user SHALL NOT appear in the returned list

#### Scenario: User has an internal member admin role

- GIVEN a user has an active role whose code is one of `manager`, `core_developer`, or `founder`
- WHEN the user query runs for `GET /pc/admin/internal-member/admin-users`
- THEN that user SHALL appear in the returned list

### Requirement: The API shall support keyword filtering

The system SHALL support optional keyword filtering for the admin-user option list.

#### Scenario: Filter by keyword

- GIVEN a client calls `GET /pc/admin/internal-member/admin-users?keyword=alice`
- WHEN matching admin users exist by username, email, role name, or role code
- THEN only matching admin users SHALL be returned

### Requirement: The API shall return the highest active admin role per user

The system SHALL return one row per admin user and use the highest active admin role when the user has multiple admin roles.

#### Scenario: User has multiple admin roles

- GIVEN a user has both `manager` and `core_developer` active roles
- WHEN the admin-user option query runs
- THEN the response SHALL include that user once
- AND the returned role information SHALL correspond to the highest active admin role
