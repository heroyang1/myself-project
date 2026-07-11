# Internal Member Role-Source Acceptance Checklist

## Scope

This checklist covers the role-source refactor under `refactor-internal-member-paid-role-source`.

Impacted endpoints:

- `GET /pc/admin/internal-member/{userId}/summary`
- `GET /pc/admin/internal-member/page`
- `GET /pc/admin/internal-member/outcome/page`
- `GET /pc/admin/internal-member/{userId}/courses`
- `GET /pc/admin/internal-member/{userId}/study-attitude`
- all single-user detail and operation endpoints guarded by `countPaidMemberByUserId`

## Preconditions

- Use a reachable base URL instead of local `8081` if local startup is still unavailable
- Use the prepared admin token
- Prepare at least these user cases if the environment allows targeted data checks:
  - `case A`: user has historical paid member orders but no active `vip` or `small_class` role
  - `case B`: user has active `vip` role
  - `case C`: user has active `small_class` role
  - `case D`: user has both active `vip` and `small_class` roles

## Acceptance Logic

1. `summary` access control

- Call `GET /pc/admin/internal-member/{userId}/summary` for `case A`
- Expected: request is rejected as not a current valid paid member
- Call the same endpoint for `case B` and `case C`
- Expected: request succeeds

2. `summary` field source

- For a successful `summary` response, verify these fields still exist:
  - `memberType`
  - `memberTypeName`
  - `planName`
  - `memberExpireTime`
  - `memberActive`
- Expected:
  - `memberType` comes from the active role code
  - `memberExpireTime` matches the active role expiration
  - `memberActive` is effectively active for returned rows
  - `planName` now reflects current member identity display, not historical order snapshot

3. `page` member set

- Call `GET /pc/admin/internal-member/page`
- Expected:
  - users from `case A` are absent
  - users from `case B` and `case C` are present
  - the response shape is unchanged

4. `outcome/page` member set

- Call `GET /pc/admin/internal-member/outcome/page`
- Expected:
  - users from `case A` are absent
  - users from `case B` and `case C` are present
  - existing outcome fields remain available

5. role priority

- Call `GET /pc/admin/internal-member/{userId}/summary` for `case D`
- Expected:
  - member selection follows the existing SQL priority rule
  - `small_class` wins over `vip`
  - if role types are the same, the later expiration wins

6. guarded detail endpoints

- Spot-check at least one of these endpoints for `case A` and one active-role case:
  - `GET /pc/admin/internal-member/{userId}/courses`
  - `GET /pc/admin/internal-member/{userId}/study-attitude`
  - `GET /pc/admin/internal-member/{userId}/employment-profile`
  - `GET /pc/admin/internal-member/{userId}/resume-summary`
  - `GET /pc/admin/internal-member/{userId}/interview-summary`
- Expected:
  - `case A` is rejected by paid-member validation
  - active-role users continue to pass access control

## Suggested Request Examples

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  "<BASE_URL>/pc/admin/internal-member/<USER_ID>/summary"
```

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  "<BASE_URL>/pc/admin/internal-member/page?pageNum=1&pageSize=20"
```

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  "<BASE_URL>/pc/admin/internal-member/outcome/page?pageNum=1&pageSize=20"
```

## Risks To Watch

- stale or dirty `osh_user_role` data can hide users that previously appeared through historical orders
- `planName` semantics changed from order snapshot to current member identity display
- environments with mixed old data may show fewer members after the switch, which is expected if roles are missing or expired

## Rollback Hint

- rollback is SQL-only for this refactor
- restore the previous member-source subqueries in:
  - `backstage-system/src/main/resources/mapper/internalmember/InternalMemberStudyMapper.xml`
  - `backstage-system/src/main/resources/mapper/internalmember/InternalMemberMapper.xml`
