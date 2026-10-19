# Design: add-internal-member-document-upload

## Overview

This change adds a minimal, stable document workflow to the internal member admin module:

1. Receive a source `.pdf` or `.docx` file
2. Extract text
3. Desensitize sensitive fields
4. Render a normalized PDF
5. Upload only the desensitized PDF to cloud storage
6. Store the resulting cloud key on the paid-member employment profile record

## API Contract

### Upload document

- Method: `POST`
- Path: `/pc/admin/internal-member/{userId}/documents`
- Content-Type: `multipart/form-data`
- Params:
  - `file` required
  - `docType` required, `resume|offer`

Response:

- `R<InternalMemberDocumentUploadVO>`

### Preview document

- Method: `GET`
- Path: `/pc/admin/internal-member/{userId}/documents/preview`
- Query:
  - `docType` required, `resume|offer`
  - `minute` optional signed URL expiry, default 30

Response:

- `R<InternalMemberDocumentPreviewVO>`

### Download document

- Method: `GET`
- Path: `/pc/admin/internal-member/{userId}/documents/download`
- Query:
  - `docType` required, `resume|offer`
  - `minute` optional signed URL expiry, default 30

Response:

- `R<InternalMemberDocumentDownloadVO>`

### Delete document

- Method: `DELETE`
- Path: `/pc/admin/internal-member/{userId}/documents`
- Query:
  - `docType` required, `resume|offer`

Response:

- `R<Void>`

## Supported Source Formats

- Supported:
  - `.pdf` when text can be extracted
  - `.docx`
- Rejected:
  - `.doc`
  - scanned/image-only PDFs

## Desensitization Scope

The upload pipeline masks:

- phone number
- email
- ID card number
- family/home address
- emergency contact
- school name
- GitHub / WeChat / QQ

## Persistence

Extend `osh_internal_member_employment_profile` with:

- `resume_file_key`
- `resume_file_name`
- `offer_file_key`
- `offer_file_name`

If a paid member has no employment profile row yet, create a lightweight shell row before document field update.

## Storage Path

Store cloud objects under:

- `common/document/internal-member/{userId}/resume/{yyyyMM}/...pdf`
- `common/document/internal-member/{userId}/offer/{yyyyMM}/...pdf`

## Failure Policy

- If extraction fails, reject upload
- If desensitization or PDF rendering fails, reject upload
- If cloud upload fails, reject upload
- If delete is requested for a missing document, reject the delete request
- Original source file is temporary only and removed after processing

## Compatibility

- Existing employment profile APIs remain unchanged except document metadata fields returned for refresh display
- Preview uses a dedicated endpoint instead of exposing signed URLs from profile detail
- Download uses a dedicated endpoint and returns an attachment-style signed URL
- Delete uses a dedicated endpoint and clears the corresponding stored file metadata
