# Design: add-internal-member-growth-story

## Overview

This change adds a dedicated user-side growth-story module under `/pc/user/internal-member/growth-story`. The implementation is template-based and deterministic in v1:

1. Read existing internal-member study and employment facts
2. Validate the current user is eligible to generate
3. Build a structured fact snapshot
4. Render a title, summary, and story content
5. Upsert the generated result into a dedicated persistence table
6. Expose self, visibility, public page, and cross-user detail APIs

## Current State

- Existing paid-member admin APIs live under `/pc/admin/internal-member`.
- Existing user-side member APIs use `/pc/user/**`.
- Existing internal-member services already expose:
  - study summary
  - study attitude
  - employment profile
  - resume summary
  - interview summary
  - exam summary
- Existing employment profile already stores uploaded `resume_file_key` and `offer_file_key`.
- There is no existing user-facing persistence model for growth-story content or visibility.

## API Contract

| Method | Path | Change | Compatibility |
|---|---|---|---|
| POST | `/pc/user/internal-member/growth-story/generate` | New | Additive |
| GET | `/pc/user/internal-member/growth-story/me` | New | Additive |
| PUT | `/pc/user/internal-member/growth-story/visibility` | New | Additive |
| GET | `/pc/user/internal-member/growth-story/page` | New | Additive |
| GET | `/pc/user/internal-member/growth-story/{targetUserId}` | New | Additive |

### Generate story

- No request body in v1
- Uses current logged-in user
- Upserts the current user's story
- Preserves existing `is_public` when regenerating an existing story

### Get my story

- Returns the current user's generated story detail
- If not generated yet, returns `null`

### Update visibility

- Request body:
  - `isPublic`: `0 | 1`
- Can only operate on the current user's own generated story
- Switching to public updates `published_time`

### Public story page

- Lists only public stories
- Current user must already have their own public story before reading others
- Target stories in the page must still satisfy employed + uploaded resume + uploaded offer

### Public story detail

- If `targetUserId` is the current user, return self detail directly
- Otherwise:
  - current user must have their own public story
  - target user must have a public generated story
  - target user must still satisfy employed + uploaded resume + uploaded offer

## Data Design

| Table / Index | Change | Migration | Rollback |
|---|---|---|---|
| `osh_internal_member_growth_story` | New table | additive create table | drop table |
| `uk_internal_member_growth_story_user_id` | unique index on `user_id` | additive create index | drop index |
| `idx_internal_member_growth_story_public_time` | index on `(is_public, published_time)` | additive create index | drop index |

### Table fields

- `id`
- `user_id`
- `story_title`
- `story_summary`
- `story_content`
- `facts_json`
- `generate_status`
- `is_public`
- `published_time`
- `create_by`
- `create_time`
- `update_by`
- `update_time`
- `delete_flag`

## Business Rules

### Generation eligibility

The current user can generate only when all conditions are met:

- paid internal-member user
- employed (`job_status = employed` or `entry_time` exists)
- uploaded resume
- uploaded offer

### Cross-user view eligibility

The current user can view other users' stories only when all conditions are met:

- current user is eligible for generation
- current user already has a generated story
- current user's story is public

### Public target eligibility

A target user story is public-viewable only when all conditions are met:

- target story exists
- target story is public
- target user is employed
- target user has uploaded resume
- target user has uploaded offer

## Story Composition

V1 story content is generated from a privacy-safe whitelist of structured facts:

- paid member plan and learning start/completion context
- learning progress and study attitude score
- resume delivery and interview progress
- exam participation summary
- employment result such as city, company, position, and entry time

V1 does not include:

- resume/original document raw text
- exact salary fields
- admin follow-record content
- admin operator/responsible information

## Middleware Impact

- Redis: no new required dependency or cache design in v1
- Kafka: none
- Elasticsearch: none
- Nacos: none
- R2/S3: reused indirectly through existing document upload prerequisite only

## Compatibility

- Existing admin internal-member APIs remain unchanged.
- Existing paid-user employment-profile and document APIs remain unchanged.
- Frontend needs a new user-side growth-story page flow.

## Test Plan

- Local compile for affected module
- Generate own story happy path
- Visibility update happy path
- `me` read when generated and when absent
- Public page blocked when current user has not shared own story
- Public detail blocked when target story is not public

## Release And Rollback

- Release steps:
  - execute SQL to create `osh_internal_member_growth_story`
  - deploy application
- Verification:
  - generate own story
  - switch to public
  - open public page
  - open another user's public detail with gating satisfied
- Rollback:
  - stop calling new APIs
  - roll back application package
  - keep table data or drop table if feature must be fully reverted
