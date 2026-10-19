#!/usr/bin/env bash
# ============================================================
# install-hooks.sh — 安装本地 Git pre-commit 钩子
# 用法: bash scripts/install-hooks.sh
# ============================================================
set -euo pipefail

HOOK_DIR=".git/hooks"
HOOK_FILE="${HOOK_DIR}/pre-commit"

if [[ ! -d ".git" ]]; then
  echo "Error: 当前目录不是 Git 仓库" >&2
  exit 1
fi

mkdir -p "$HOOK_DIR"

cat > "$HOOK_FILE" << 'HOOK'
#!/usr/bin/env bash
# Pre-commit hook: 运行 code-quality-guard 检查
# 安装方式: bash scripts/install-hooks.sh

echo "Running code-quality-guard..."
bash scripts/code-quality-guard.sh
HOOK

chmod +x "$HOOK_FILE"
echo "✓ pre-commit hook 已安装到 ${HOOK_FILE}"
echo "  每次 git commit 前将自动运行质量检查"
echo "  如需临时跳过: git commit --no-verify"
