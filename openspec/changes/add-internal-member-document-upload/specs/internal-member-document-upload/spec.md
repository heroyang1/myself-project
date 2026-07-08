# Spec: internal-member-document-upload

## ADDED Requirements

### Requirement: Internal member admin module shall upload desensitized documents only

The system SHALL support paid-member document upload in the internal member admin module and SHALL persist only desensitized PDF output to cloud storage.

#### Scenario: Upload resume document

- GIVEN an admin calls `POST /pc/admin/internal-member/{userId}/documents`
- AND `docType=resume`
- AND the source file is a supported `.pdf` or `.docx`
- WHEN text extraction, desensitization, and PDF generation succeed
- THEN the system SHALL upload only the desensitized PDF to cloud storage
- AND the system SHALL save the resulting file key and file name for the user

#### Scenario: Upload fails during desensitization

- GIVEN an admin calls `POST /pc/admin/internal-member/{userId}/documents`
- WHEN text extraction or PDF generation fails
- THEN the system SHALL reject the upload
- AND the system SHALL NOT persist a cloud artifact reference for the user

### Requirement: Internal member admin module shall preview the latest uploaded document

The system SHALL provide a signed preview URL for the latest uploaded document of a requested type.

#### Scenario: Preview uploaded offer document

- GIVEN an admin calls `GET /pc/admin/internal-member/{userId}/documents/preview?docType=offer`
- WHEN the user has an uploaded offer document
- THEN the system SHALL return a signed preview URL
- AND the response SHALL include the stored file key and file name

### Requirement: Internal member admin module shall download the latest uploaded document

The system SHALL provide a signed download URL for the latest uploaded document of a requested type.

#### Scenario: Download uploaded resume document

- GIVEN an admin calls `GET /pc/admin/internal-member/{userId}/documents/download?docType=resume`
- WHEN the user has an uploaded resume document
- THEN the system SHALL return a signed download URL
- AND the response SHALL include the stored file key and file name

### Requirement: Internal member admin module shall delete the latest uploaded document

The system SHALL allow an admin to remove the latest uploaded document of a requested type and clear the stored metadata.

#### Scenario: Delete uploaded offer document

- GIVEN an admin calls `DELETE /pc/admin/internal-member/{userId}/documents?docType=offer`
- WHEN the user has an uploaded offer document
- THEN the system SHALL clear the stored file key and file name for that document type
- AND the system SHALL remove the cloud object for that document

### Requirement: Unsupported source formats shall be rejected

The system SHALL reject unsupported or non-text-extractable source documents.

#### Scenario: Upload legacy doc file

- GIVEN an admin uploads a `.doc` file
- WHEN the upload request is processed
- THEN the system SHALL reject the file as unsupported

#### Scenario: Upload scanned PDF

- GIVEN an admin uploads an image-only/scanned PDF with no extractable text
- WHEN the upload request is processed
- THEN the system SHALL reject the file
