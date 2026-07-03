# Spec: book-content-access-control

## MODIFIED Requirements

### Requirement: Book public endpoints are limited to discovery data

The system SHALL allow anonymous access only to public book discovery data such as list, search, and tag list endpoints.

#### Scenario: Anonymous user browses public book list

- GIVEN a request without a token
- WHEN the request calls `/pc/book/page`, `/pc/book/search`, or `/pc/book/getTagList`
- THEN the system allows the request to reach the public discovery handler.

#### Scenario: Anonymous user requests protected book content metadata

- GIVEN a request without a token
- WHEN the request calls `/pc/book/getById`, `/pc/book/detail`, or `/pc/book/menus`
- THEN the system rejects the request before returning book detail, menu, text content, or comic image URLs.

### Requirement: Book metadata endpoints do not expose readable content

The system SHALL NOT return chapter body content, comic page arrays, or comic image URLs from book detail or menu metadata endpoints.

#### Scenario: Authenticated user opens book detail

- GIVEN an authenticated user
- WHEN the user calls `/pc/book/getById` with `forEdit=false` or no `forEdit`
- THEN the response may include book metadata and chapter catalog fields
- AND the response does not include chapter `content`, `pages`, or `imageUrl` values.

#### Scenario: Authenticated user opens book menu

- GIVEN an authenticated user
- WHEN the user calls `/pc/book/menus`
- THEN the response may include book metadata and chapter catalog fields
- AND the response does not include chapter `content`, `pages`, or `imageUrl` values.

### Requirement: Chapter content endpoint remains access-controlled

The system SHALL return chapter text content or comic pages only from the chapter content endpoint after chapter readability checks pass.

#### Scenario: Unpurchased paid chapter is requested

- GIVEN an authenticated user without purchase access to a non-free paid chapter
- WHEN the user calls `/pc/book/detail`
- THEN the system rejects the request and does not return text content or comic image URLs.

#### Scenario: Purchased or free chapter is requested

- GIVEN an authenticated user who is allowed to read the chapter
- WHEN the user calls `/pc/book/detail`
- THEN the system returns the allowed chapter content and comic pages when applicable.

### Requirement: Object preview signing is manager-only

The system SHALL restrict bulk object preview URL signing to users with level 4 or above.

#### Scenario: Normal user signs known object path

- GIVEN an authenticated normal user below level 4
- WHEN the user calls `/pc/upload/preview-urls`
- THEN the system rejects the request before returning a signed preview URL.

#### Scenario: Manager signs object paths for editing

- GIVEN an authenticated user with level 4 or above
- WHEN the user calls `/pc/upload/preview-urls`
- THEN the system may return signed preview URLs for the requested paths.
