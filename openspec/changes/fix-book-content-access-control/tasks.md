# Tasks: fix-book-content-access-control

## Implementation

- [x] Document book content access control change.
- [x] Remove `@Anonymous` from non-list book endpoints.
- [x] Keep anonymous access for list/search/tag endpoints only.
- [x] Sanitize normal detail chapter lists by removing content and comic pages.
- [x] Sanitize menu chapter lists by removing content and comic pages.
- [x] Keep editor detail responses protected by manager level.
- [x] Restrict bulk preview URL signing to level 4 or above.

## Verification

- [x] Add or update backend tests for detail/menu sanitization.
- [x] Run targeted backend tests.
- [x] Report remaining compatibility risks.
