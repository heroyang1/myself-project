# Spec: internal-member-growth-story

## ADDED Requirements

### Requirement: Generate Own Growth Story

The system SHALL allow a paid internal-member user to generate a persisted personal growth story from existing structured internal-member data.

#### Scenario: Eligible user generates own story

- GIVEN the current user is a paid internal-member user
- AND the current user is already employed
- AND the current user has uploaded both resume and offer
- WHEN the current user calls `POST /pc/user/internal-member/growth-story/generate`
- THEN the system creates or updates the user's persisted growth story
- AND the response succeeds

#### Scenario: Ineligible user tries to generate story

- GIVEN the current user is missing employment status or required uploaded documents
- WHEN the current user calls `POST /pc/user/internal-member/growth-story/generate`
- THEN the system rejects the request with a clear business error

### Requirement: Read Own Growth Story

The system SHALL expose the current user's persisted growth story detail.

#### Scenario: User reads existing own story

- GIVEN the current user has already generated a story
- WHEN the current user calls `GET /pc/user/internal-member/growth-story/me`
- THEN the system returns the current user's story detail

#### Scenario: User reads own story before generation

- GIVEN the current user has not generated a story yet
- WHEN the current user calls `GET /pc/user/internal-member/growth-story/me`
- THEN the system returns `null`

### Requirement: Manage Story Visibility

The system SHALL allow a user to toggle whether their own growth story is public.

#### Scenario: User publishes own story

- GIVEN the current user has already generated a story
- WHEN the current user calls `PUT /pc/user/internal-member/growth-story/visibility` with `isPublic=1`
- THEN the system marks the story as public
- AND records `published_time`

#### Scenario: User hides own story

- GIVEN the current user has already generated a story
- WHEN the current user calls `PUT /pc/user/internal-member/growth-story/visibility` with `isPublic=0`
- THEN the system marks the story as non-public

### Requirement: View Other Public Growth Stories

The system SHALL require a share-first rule before a user can read other users' public growth stories.

#### Scenario: Shared user reads public story page

- GIVEN the current user is eligible to generate a story
- AND the current user has already generated and published their own story
- WHEN the current user calls `GET /pc/user/internal-member/growth-story/page`
- THEN the system returns only public stories from eligible employed users with uploaded resume and offer

#### Scenario: User without own public story reads others

- GIVEN the current user has not published their own story
- WHEN the current user calls `GET /pc/user/internal-member/growth-story/page`
- THEN the system rejects the request with a clear business error

#### Scenario: Shared user reads target public story detail

- GIVEN the current user has already generated and published their own story
- AND the target user has a public generated story
- AND the target user is employed
- AND the target user has uploaded both resume and offer
- WHEN the current user calls `GET /pc/user/internal-member/growth-story/{targetUserId}`
- THEN the system returns the target user's story detail

#### Scenario: Target story is not public

- GIVEN the target user has not published their story
- WHEN the current user calls `GET /pc/user/internal-member/growth-story/{targetUserId}`
- THEN the system rejects the request with a clear business error
