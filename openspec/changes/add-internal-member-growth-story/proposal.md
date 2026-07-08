# Proposal: add-internal-member-growth-story

## Background

The paid-user internal-member module currently exposes study progress, employment profile, resume delivery, interview, exam, and document upload capabilities, but it does not provide a user-facing growth-story feature.

The new capability must let an employed paid user generate a reusable personal growth story from existing structured data, and must gate cross-user viewing behind an explicit share-first rule.

## Goals

- Add a user-side growth-story generation capability for paid internal members.
- Persist generated story content instead of generating it on every read.
- Allow users to toggle whether their own story is public.
- Allow users to view public stories from other users only after they have generated and shared their own story.
- Reuse existing internal-member structured data as the v1 story source.

## Non-Goals

- This change does not add AI freeform generation.
- This change does not expose admin-only follow record details to other users.
- This change does not introduce a generic social feed, like, comment, or share-count system.
- This change does not change existing admin internal-member APIs.

## User Impact

- Paid users can generate their own growth story under the user-side internal-member module.
- Paid users can manage whether their generated story is public.
- Paid users who have shared their own story can browse and view other public growth stories.
- Users who are not employed or who have not uploaded both resume and offer cannot generate a story.

## Risks

- Story quality depends on the completeness of existing structured internal-member data.
- Public story exposure requires careful filtering so internal/admin-only fields are not leaked.
- Existing history data may be sparse for some paid users, producing shorter stories.

## Acceptance Criteria

- [ ] Add `POST /pc/user/internal-member/growth-story/generate`.
- [ ] Add `GET /pc/user/internal-member/growth-story/me`.
- [ ] Add `PUT /pc/user/internal-member/growth-story/visibility`.
- [ ] Add `GET /pc/user/internal-member/growth-story/page`.
- [ ] Add `GET /pc/user/internal-member/growth-story/{targetUserId}`.
- [ ] Generation requires the current user to be a paid member, employed, and to have uploaded both resume and offer.
- [ ] Viewing others requires the current user to have generated and published their own story.
- [ ] Only public stories from eligible employed users are visible to others.
- [ ] Story data is persisted in a dedicated table with rollback-safe SQL.
