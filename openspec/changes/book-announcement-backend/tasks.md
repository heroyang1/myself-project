# Tasks: book-announcement-backend

## Preparation

- [x] Read backend `AGENTS.md`.
- [x] Read backend SDD workflow docs.
- [x] Read `openspec/project.md`.

## Implementation

- [x] Add `BOOK` to announcement module enum.
- [x] Add book announcement query endpoints.
- [x] Insert book dynamic announcement after create.
- [x] Broadcast book announcement refresh after create.
- [x] Keep endpoint response compatible with `AnnouncementMarqueeVO`.

## Verification

- [x] Compile backend module.
- [x] Smoke test book announcement endpoints.
- [x] Verify created announcement data in MySQL.
- [x] Verify book creation refresh event behavior.
