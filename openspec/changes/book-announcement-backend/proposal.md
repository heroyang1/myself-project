# Proposal: book-announcement-backend

## Background

The frontend book list page already requests `/pc/book/announcement/notice` and `/pc/book/announcement/dynamic`, but the backend does not provide these endpoints. Newly created books and comics are therefore not visible in the shared announcement board even though the platform has a unified `osh_announcement` table.

## Goals

- Provide book module announcement query endpoints for notice and dynamic channels.
- Write a dynamic announcement when a book or comic is created successfully.
- Broadcast the standard book announcement refresh event after a new book/comic announcement is published.
- Reuse the existing `osh_announcement` table and announcement VO/query conventions.

## Non-Goals

- Do not add a new announcement table.
- Do not redesign the shared announcement component.
- Do not change book payment, read access, or edit permissions.

## User Impact

- Users can see newly created books/comics in the book module announcement board.
- Existing book create/edit/read flows keep their current behavior.

## Risks

- Compatibility risk: low. New endpoints are additive.
- Data risk: low. Only one announcement row is inserted for newly created books.
- Performance risk: low. Queries are limited and indexed by existing announcement columns.
- Security risk: low. Announcement queries expose public title/link metadata only.
- Rollback risk: low. Remove the new endpoints and skip announcement insertion.

## Acceptance

- [ ] `GET /pc/book/announcement/notice` returns published `book` channel 1 announcements.
- [ ] `GET /pc/book/announcement/dynamic` returns published `book` channel 2 announcements.
- [ ] Creating an ebook writes a `book` dynamic announcement linking to the detail page.
- [ ] Creating a comic writes a `book` dynamic announcement linking to the detail page.
- [ ] Creating a book or comic broadcasts `ANNOUNCEMENT_REFRESH` for module `book`.
