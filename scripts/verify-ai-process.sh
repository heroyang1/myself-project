#!/usr/bin/env bash
set -euo pipefail

required_files=(
  "AGENTS.md"
  "CONTRIBUTING.md"
  "CODE_REVIEW.md"
  "SECURITY.md"
  "CLAUDE.md"
  "GEMINI.md"
  ".cursorrules"
  "docs/ai/README.md"
  "docs/ai/01-newcomer-guide.md"
  "docs/ai/02-workflow.md"
  "docs/ai/03-codex-harness.md"
  "openspec/project.md"
  "openspec/templates/proposal.md"
  "openspec/templates/design.md"
  "openspec/templates/tasks.md"
  "openspec/templates/spec.md"
  ".github/PULL_REQUEST_TEMPLATE.md"
)

for file in "${required_files[@]}"; do
  if [[ ! -s "$file" ]]; then
    echo "Missing required AI process file: $file" >&2
    exit 1
  fi
done

# ── 基线解析：优先 BASE_REF，其次 GITHUB_BASE_REF；不再硬编码具体 qa 分支 ──
base_ref="${BASE_REF:-}"
if [[ -z "$base_ref" && -n "${GITHUB_BASE_REF:-}" ]]; then
  base_ref="origin/${GITHUB_BASE_REF}"
fi
if [[ -n "$base_ref" ]] && git rev-parse --verify "$base_ref" >/dev/null 2>&1; then
  diff_base="$base_ref"
  changed_files="$(git diff --name-only "$diff_base"...HEAD || true)"
else
  diff_base="HEAD~1"
  echo "WARN: base ref '${base_ref:-<unset>}' unavailable, falling back to HEAD~1 (may under-report scope; set BASE_REF explicitly)." >&2
  changed_files="$(git diff --name-only HEAD~1...HEAD || true)"
fi
untracked_files="$(git ls-files --others --exclude-standard || true)"
changed_files="$(printf '%s\n%s\n' "$changed_files" "$untracked_files" | sed '/^$/d' | sort -u)"

# ── 归档守卫：openspec/changes/archive/ 只读——禁止修改/删除基线分支上已归档的内容（新增归档允许）。──
# 注：范围 diff 为净差异，同一 PR 内“先新增后篡改”会折叠为 A，属于归档动作本身，放行。
tampered_archive="$(git diff --name-only --diff-filter=MD "$diff_base"...HEAD 2>/dev/null | grep '^openspec/changes/archive/' || true)"
if [[ -n "$tampered_archive" ]]; then
  echo "Archive guard violated: archived OpenSpec changes are read-only (fix history via a NEW change):" >&2
  echo "$tampered_archive" >&2
  exit 1
fi

# ── change 四件套完整性（archive/ 归档区不参与 change 校验；grep -v 空输入需 || true 防护 pipefail）──
change_dirs="$(echo "$changed_files" | sed -n 's#^\(openspec/changes/[^/][^/]*\)/.*#\1#p' | { grep -v '^openspec/changes/archive$' || true; } | sort -u)"
if [[ -n "$change_dirs" ]]; then
  bad=0
  while IFS= read -r change_dir; do
    [[ -z "$change_dir" ]] && continue
    for file in proposal.md design.md tasks.md; do
      if [[ ! -s "$change_dir/$file" ]]; then
        echo "OpenSpec change missing $file: $change_dir" >&2
        bad=1
      fi
    done
    if ! find "$change_dir/specs" -type f -name 'spec.md' -size +0c >/dev/null 2>&1; then
      echo "OpenSpec change missing specs/**/spec.md: $change_dir" >&2
      bad=1
    fi
  done <<< "$change_dirs"
  exit "$bad"
fi

# ── 保护域：版本基线(pom)/DDL(sql)/运行时配置(application*) 不可豁免，必须携带 OpenSpec change ──
protected_regex='(^|/)pom\.xml$|^sql/|(^|/)application[^/]*\.(yml|yaml|properties)$'
protected_hits="$(echo "$changed_files" | grep -E "$protected_regex" || true)"
if [[ -n "$protected_hits" ]]; then
  echo "Protected-domain files changed without an OpenSpec change (exemption does NOT apply):" >&2
  echo "$protected_hits" >&2
  cat >&2 <<'EOF'
Protected domains: pom.xml (tech baseline), sql/ (DDL), application*.yml|properties (runtime config).
Create openspec/changes/<change-id>/ with proposal.md, design.md, tasks.md, specs/**/spec.md.
EOF
  exit 1
fi

if [[ "${ALLOW_OPENSPEC_EXEMPT:-}" == "true" ]]; then
  echo "OpenSpec change not found, but ALLOW_OPENSPEC_EXEMPT=true."
  exit 0
fi

cat >&2 <<'EOF'
No OpenSpec change detected in this PR.

If this is a tiny non-behavioral change, set ALLOW_OPENSPEC_EXEMPT=true in CI only after the PR body contains:

OpenSpec-Exempt: true
Reason: <clear reason>

Note: changes touching protected domains (pom.xml / sql/ / application*.yml) can never be exempt.
EOF
exit 1
