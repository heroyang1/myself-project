# Spec: internal-member-paid-role-source

## MODIFIED Requirements

### Requirement: paid-member validation uses active roles

The system SHALL determine whether an internal-member user is a paid member from current active member roles rather than from historical member orders.

#### Scenario: summary accepts a user with an active member role

- GIVEN a user has an active `vip` or `small_class` role
- AND the role record has `expire_time > NOW()`
- WHEN an admin calls `GET /pc/admin/internal-member/{userId}/summary`
- THEN the request passes paid-member validation

#### Scenario: historical orders alone are no longer sufficient

- GIVEN a user has historical member orders
- AND the user has no current active `vip` or `small_class` role
- WHEN an admin calls `GET /pc/admin/internal-member/{userId}/summary`
- THEN the system treats the user as not being a current valid paid member

#### Scenario: guarded detail endpoints follow the same validation rule

- GIVEN a user has historical member orders
- AND the user has no current active `vip` or `small_class` role
- WHEN an admin calls a guarded detail endpoint such as `GET /pc/admin/internal-member/{userId}/courses`
- THEN the request is rejected by paid-member validation

### Requirement: summary member fields come from active roles

The system SHALL return summary member fields from current active member roles while preserving the existing response shape.

#### Scenario: summary keeps the same fields

- GIVEN a user has a current active member role
- WHEN an admin calls `GET /pc/admin/internal-member/{userId}/summary`
- THEN the response still contains `memberType`, `memberTypeName`, `planName`, `memberExpireTime`, and `memberActive`
- AND those fields come from the active role source instead of `osh_member_order`

### Requirement: page and outcome member sets use active roles

The system SHALL build internal-member list and outcome list member sets from current active member roles rather than from historical member orders.

#### Scenario: page excludes users with only historical orders

- GIVEN a user has historical member orders only
- AND the user has no active `vip` or `small_class` role
- WHEN an admin calls `GET /pc/admin/internal-member/page`
- THEN the user is not included in the result set

#### Scenario: outcome page excludes users with only historical orders

- GIVEN a user has historical member orders only
- AND the user has no active `vip` or `small_class` role
- WHEN an admin calls `GET /pc/admin/internal-member/outcome/page`
- THEN the user is not included in the result set

#### Scenario: page keeps existing output fields

- GIVEN a user has a current active `vip` or `small_class` role
- WHEN an admin calls `GET /pc/admin/internal-member/page`
- THEN the response still includes `memberType`, `memberTypeName`, `planName`, `memberExpireTime`, and `memberActive`
- AND those fields come from the active role source
