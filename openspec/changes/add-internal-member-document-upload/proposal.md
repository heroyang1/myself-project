# Proposal: add-internal-member-document-upload

## Background

The internal member admin module currently stores structured employment profile fields, but it does not support uploading resume or offer documents for paid members.

The new capability must ensure sensitive information is desensitized before cloud storage. Original source files must not become persistent cloud artifacts.

## Goals

- Add a paid-member document upload endpoint under the internal member admin module.
- Add a preview endpoint for uploaded documents.
- Add a download endpoint for uploaded documents.
- Add a delete endpoint for uploaded documents.
- Support `resume` and `offer` document types.
- Persist only desensitized PDF artifacts to cloud storage.
- Reject upload when desensitization or PDF generation fails.

## Non-Goals

- No generic document center is added in this change.
- No OCR support for scanned PDFs is included.
- No support for legacy `.doc` files is included.
- No ordinary user module changes are included.

## User Impact

- Admins can upload resume and offer documents for paid members.
- Frontend can preview the latest uploaded document through a signed URL.
- Frontend can download the latest uploaded document through a dedicated signed URL.
- Frontend can delete the latest uploaded document of a chosen type.
- Sensitive fields are removed before cloud persistence.

## Risks

- Text-only extraction is required; scanned PDFs are not supported in v1.
- The generated PDF is a normalized text PDF rather than a layout-preserving copy.
- PDF generation depends on a usable CJK font being available on the host.

## Acceptance Criteria

- [ ] Add `POST /pc/admin/internal-member/{userId}/documents`.
- [ ] Add `GET /pc/admin/internal-member/{userId}/documents/preview`.
- [ ] Add `GET /pc/admin/internal-member/{userId}/documents/download`.
- [ ] Add `DELETE /pc/admin/internal-member/{userId}/documents`.
- [ ] Upload supports `docType=resume|offer`.
- [ ] Upload supports `.pdf` and `.docx` only.
- [ ] Cloud storage keeps only the desensitized PDF output.
- [ ] Upload fails when desensitization or PDF generation fails.
