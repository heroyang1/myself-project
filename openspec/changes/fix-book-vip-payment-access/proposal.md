# Proposal: fix-book-vip-payment-access

## Background

The book chapter access logic currently allows reading paid VIP book chapters when the user's role level is greater than or equal to the book level. For VIP paid resources, access must be granted by purchase records and paid order completion instead of role level.

## Goals

- Require purchase for non-free chapters of VIP paid books and comics.
- Keep free trial chapter access unchanged.
- Keep role-level access for non-paid restricted books.
- Keep existing points deduction behavior for book purchase checkout.

## Non-Goals

- Do not change non-book payment flows.
- Do not add new tables or SQL migration.
- Do not alter comic storage or bookmark behavior.

## Acceptance

- [ ] `level=2` and `price>0` books require purchase for non-free chapters.
- [ ] Free trial chapters remain readable before purchase.
- [ ] Non-paid restricted books can still use role-level access as before.
- [ ] Existing book purchase checkout continues to support points deduction by default.
