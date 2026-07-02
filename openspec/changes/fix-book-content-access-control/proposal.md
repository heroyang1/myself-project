# Proposal: fix-book-content-access-control

## Background

The book detail endpoint currently exposes chapter content and comic page image URLs in `book_details`. Because some book endpoints were annotated with `@Anonymous`, an unauthenticated request could retrieve paid comic page URLs from `/pc/book/getById` without passing purchase or level checks.

## Goals

- Keep anonymous access only for public listing/search/tag data in the book module.
- Require login for book detail, chapter content, and chapter menu endpoints.
- Prevent detail and menu endpoints from returning chapter body content or comic page image URLs.
- Keep chapter reading content behind the existing chapter access checks.
- Restrict bulk object preview URL signing to manager-level users so normal users cannot sign arbitrary known storage paths.

## Non-Goals

- Do not change database tables or SQL migrations.
- Do not redesign payment checkout.
- Do not replace object storage URLs with signed proxy delivery in this change.
- Do not change unrelated course, tool, order, or redeem modules.

## User Impact

- Anonymous users can still browse book lists, search results, and tags.
- Users must log in before opening a detail, menu, or chapter content endpoint.
- Detail pages can still render book metadata and chapter catalogs.
- Reading pages must load actual chapter content only through `/pc/book/detail`.

## Risks

- Compatibility risk: medium. Any frontend code that expected `getById.book_details[].content/pages` must use the chapter content endpoint instead.
- Data risk: low. No schema or persistence changes.
- Performance risk: low. Detail and menu responses become smaller.
- Security risk: reduced. Paid content is no longer exposed through metadata endpoints.
- Rollback risk: low. Restore previous annotations and response assembly if emergency rollback is required.

## Acceptance

- [ ] `/pc/book/page`, `/pc/book/search`, and `/pc/book/getTagList` remain anonymous-accessible.
- [ ] `/pc/book/getById`, `/pc/book/detail`, and `/pc/book/menus` are not anonymous-accessible.
- [ ] `/pc/book/getById` does not return chapter `content`, `pages`, or `imageUrl` for normal detail requests.
- [ ] `/pc/book/menus` does not return chapter `content`, `pages`, or `imageUrl`.
- [ ] `/pc/book/detail` returns chapter content/pages only after `canReadChapter` allows access.
- [ ] `/pc/upload/preview-urls` requires user level 4 or above.
