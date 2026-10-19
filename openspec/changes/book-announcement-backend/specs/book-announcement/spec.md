# Spec: book-announcement

## ADDED Requirements

### Requirement: Book module announcement endpoints

The system SHALL expose notice and dynamic announcement endpoints for the book module using the shared announcement table.

#### Scenario: Query book notices

- GIVEN published `osh_announcement` rows with `resource_type = 'book'` and `channel = 1`
- WHEN a client calls `GET /pc/book/announcement/notice`
- THEN the system returns the latest matching announcements.

#### Scenario: Query book dynamics

- GIVEN published `osh_announcement` rows with `resource_type = 'book'` and `channel = 2`
- WHEN a client calls `GET /pc/book/announcement/dynamic`
- THEN the system returns the latest matching announcements.

### Requirement: Book creation publishes dynamic announcement

The system SHALL publish a book dynamic announcement after a book or comic is created.

#### Scenario: Create ebook

- GIVEN a manager creates an ebook
- WHEN the create operation succeeds
- THEN the system inserts a published `book` dynamic announcement linked to the ebook detail page.

#### Scenario: Create comic

- GIVEN a manager creates a comic
- WHEN the create operation succeeds
- THEN the system inserts a published `book` dynamic announcement linked to the comic detail page.

### Requirement: Book creation refreshes announcement board

The system SHALL broadcast the standard book announcement refresh event after a book or comic creation publishes a dynamic announcement.

#### Scenario: Refresh book announcement board

- GIVEN a manager creates a book or comic
- WHEN the create operation succeeds and the dynamic announcement is published
- THEN the system broadcasts an `ANNOUNCEMENT_REFRESH` event with module `book` and action `refresh`.
