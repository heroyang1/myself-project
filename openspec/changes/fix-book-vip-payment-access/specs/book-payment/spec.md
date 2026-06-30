# Spec: book-payment

## Requirements

### Requirement: VIP paid book chapter access must require purchase

For books and comics where `level=2` and `price>0`, non-free chapters SHALL only be readable when the current user has a purchased relation for that book.

#### Scenario: unpaid VIP paid chapter

- **GIVEN** a VIP paid book chapter marked non-free
- **AND** the current user has not purchased the book
- **WHEN** the chapter detail endpoint is requested
- **THEN** the backend rejects access

#### Scenario: free trial chapter

- **GIVEN** a VIP paid book chapter marked free
- **WHEN** the chapter detail endpoint is requested
- **THEN** the backend returns the chapter content

#### Scenario: purchased VIP paid chapter

- **GIVEN** a VIP paid book chapter marked non-free
- **AND** the current user has purchased the book
- **WHEN** the chapter detail endpoint is requested
- **THEN** the backend returns the chapter content
