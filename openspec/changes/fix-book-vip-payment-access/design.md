# Design: fix-book-vip-payment-access

## Access Rule

Book chapter access keeps the existing order:

1. Free chapter is readable.
2. Free book is readable.
3. VIP paid book (`level=2` and `price>0`) requires `user_book_relation.purchased=1`.
4. Other restricted non-paid books can use role-level access.

This makes paid access authoritative on backend and prevents frontend role-level shortcuts from granting paid content.

## Purchase

Book purchase continues to call the unified order checkout service. Points deduction remains enabled by default, so users with points automatically deduct as much as possible and pay cash only for the remainder.
