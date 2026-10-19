# Tasks: add-internal-member-growth-story

## Preparation

- [x] Review `openspec/project.md` and relevant AI conventions
- [x] Confirm this is a medium change requiring OpenSpec
- [x] Confirm reusable internal-member data sources and user-side route style

## Implementation

- [x] Add OpenSpec proposal, design, tasks, and spec
- [x] Add SQL migration for `osh_internal_member_growth_story`
- [x] Add growth-story DTOs and VOs
- [x] Add user-side controller under `/pc/user/internal-member/growth-story`
- [x] Add dedicated growth-story service and mapper
- [x] Implement generation eligibility and public-view gating
- [x] Implement template-based story rendering and upsert persistence

## Verification

- [x] Compile backend modules
- [x] Verify affected API paths and response contracts
- [x] Review compatibility with existing internal-member module

## PR

- [ ] Link OpenSpec change in PR
- [ ] Record verification result
- [ ] Record risk and rollback notes
