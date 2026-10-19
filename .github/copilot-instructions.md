# Copilot Instructions

Shared policy source: `AGENTS.md` at repository root. Keep this file aligned with that source.

## SDD / OpenSpec workflow

Before medium or large code changes, read:

1. `docs/ai/README.md`
2. `docs/ai/02-workflow.md`
3. `openspec/project.md`
4. The active change under `openspec/changes/<change-id>/`

Do not change API contracts, permissions, data models, middleware configuration, deployment behavior, or storage behavior without an OpenSpec change, unless the PR explicitly declares `OpenSpec-Exempt: true` with a clear reason.

Protected domains can never be exempt: PRs touching `pom.xml`, `sql/`, or `application*.yml|properties` must carry an OpenSpec change. `openspec/changes/archive/` is read-only; fix history via a new change.

## Hard CI gates (enforced by .github/workflows/pr-check.yml)

1. OpenSpec gate + governance-file presence + protected-domain/archive guards (`scripts/verify-ai-process.sh`).
2. DDL↔entity/VO consistency (`scripts/code-quality-guard.sh`).
3. JDK 17 build + core test suites.
4. ArchUnit guardrails: `LayeredArchitectureTest`, `CodingConventionArchitectureTest`.
5. Secret scan (gitleaks).
6. PR title must match Conventional Commits `<type>(<scope>)?: <subject>`.

Before starting work in a session, run the preflight self-check: `powershell -File scripts/preflight.ps1` (branch, dirty tree, active change-id, protected-domain hints). Full agent checklist: `docs/ai/03-codex-harness.md`.

## ZenTao MCP token guard

Before any operation that uses `mcpServers.zentao` from `MCP.json`, run the token guard script first:

```powershell
powershell -ExecutionPolicy Bypass -File .\bin\ensure_zentao_mcp_token.ps1 -ConfigPath .\MCP.json
```

Rules:

- If token exists and is valid, continue MCP operation.
- If token is empty or expired, request account/password and refresh token first.
- Do not skip this step for ZenTao MCP calls.

## Auto workflow for MCP calls

When the current task needs ZenTao MCP (`mcpServers.zentao`):

1. Always execute token guard command first.
2. If guard returns valid token, continue ZenTao MCP operation directly.
3. If guard asks for login, prompt user for account/password and complete refresh.
4. Only after successful refresh+verification, continue ZenTao MCP operation.

## Cross-agent consistency

`AGENTS.md` is the single policy source. The same workflow is mirrored in:

- `CLAUDE.md`, `GEMINI.md`, `.cursorrules` — pointer-only files; keep all rule content in `AGENTS.md`.
- This file must stay aligned with `AGENTS.md`; CI (`scripts/verify-ai-process.sh`) fails if any of these files is missing or empty.
