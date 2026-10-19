# ============================================================
# preflight.ps1 — 会话起点自检（Codex/Harness "每次开发前"清单的机械化产出）
#
# 用法:
#   powershell -ExecutionPolicy Bypass -File .\scripts\preflight.ps1
#   powershell -ExecutionPolicy Bypass -File .\scripts\preflight.ps1 -BaseRef origin/qa/20260708
#
# 对应 docs/ai/03-codex-harness.md "每次开发前"确认项：
#   分支 / 未提交改动 / 本地改动保护 / OpenSpec change 存在性 / 受管辖域触碰提示
# 本脚本只读不改，不做任何破坏性 git 操作。
# 注意：本文件必须保存为 UTF-8 with BOM，否则 PowerShell 5.1 会按 ANSI 解码导致中文乱码解析失败。
# ============================================================
param(
    [string]$BaseRef = ''
)

# git 原生命令常向 stderr 写进度/警告，EAP=Stop 会被 PS5.1 包成异常；改由显式检查兜底
$ErrorActionPreference = 'Continue'
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

Write-Host '=== osh-backend preflight ===' -ForegroundColor Cyan

# ---------- 1. 当前分支 ----------
$branch = (git rev-parse --abbrev-ref HEAD) -join ''
Write-Host "[branch]  $branch"

# ---------- 2. 工作区状态（保护本地改动） ----------
$dirty = @(git status --short)
if ($dirty.Count -gt 0) {
    Write-Host "[workspace] 有 $($dirty.Count) 处未提交改动 —— 保留用户本地修改，禁止 reset --hard / checkout --" -ForegroundColor Yellow
    $dirty | Select-Object -First 10 | ForEach-Object { Write-Host "            $_" }
    if ($dirty.Count -gt 10) { Write-Host "            ... 共 $($dirty.Count) 项" }
} else {
    Write-Host '[workspace] 干净'
}

# ---------- 3. 基线解析与落后检查 ----------
if (-not $BaseRef) { $BaseRef = $env:BASE_REF }
if (-not $BaseRef) {
    # 默认取最新的 origin/qa/* 分支作为基线（与仓库 release 流一致）
    $qa = @(git branch -r --list 'origin/qa/*' | ForEach-Object { $_.Trim() } | Sort-Object)
    if ($qa.Count -gt 0) { $BaseRef = $qa[-1] } else { $BaseRef = 'origin/main' }
}
git fetch origin --quiet 2>$null | Out-Null   # 网络失败不阻断，用本地已有 remote ref
if (git rev-parse --verify "$BaseRef" 2>$null) {
    $counts = @((git rev-list --left-right --count "$BaseRef...HEAD") -split "`t")
    $behind = [int]$counts[0]; $ahead = [int]$counts[1]
    Write-Host "[baseline] $BaseRef  |  落后 $behind / 领先 $ahead 提交"
    if ($behind -gt 0) {
        Write-Host "           提示: 建议先 git pull --rebase 同步基线（冲突即停，勿强行覆盖）" -ForegroundColor Yellow
    }
} else {
    Write-Host "[baseline] 基线 $BaseRef 不可解析 —— 用 HEAD~1 估计变更范围（CI 会显式传 BASE_REF）" -ForegroundColor Yellow
}

# ---------- 4. 本分支触碰的 OpenSpec change 与完整性预检 ----------
$changed = @(git diff --name-only "$BaseRef...HEAD" 2>$null) + @((git ls-files --others --exclude-standard))
$changed = $changed | Where-Object { $_ } | Sort-Object -Unique
$changeDirs = $changed | Select-String -Pattern '^openspec/changes/([^/]+)/' | ForEach-Object { $_.Matches[0].Groups[1].Value } | Where-Object { $_ -ne 'archive' } | Sort-Object -Unique
if ($changeDirs.Count -gt 0) {
    foreach ($cid in $changeDirs) {
        $missing = @()
        foreach ($f in @('proposal.md', 'design.md', 'tasks.md')) {
            $p = Join-Path "openspec/changes/$cid" $f
            if (-not (Test-Path $p) -or (Get-Item $p).Length -eq 0) { $missing += $f }
        }
        $specs = @(Get-ChildItem -Recurse -Filter 'spec.md' "openspec/changes/$cid/specs" -ErrorAction SilentlyContinue) | Where-Object { $_.Length -gt 0 }
        if ($specs.Count -eq 0) { $missing += 'specs/**/spec.md' }
        if ($missing.Count -gt 0) {
            Write-Host "[openspec]  $cid 缺完整四件套: $($missing -join ', ') —— CI 会拦截" -ForegroundColor Red
        } else {
            Write-Host "[openspec]  $cid 四件套齐全"
        }
    }
} else {
    Write-Host '[openspec]  本分支未携带 change —— 若为行为/接口/数据/中间件改动须先建四件套；纯文档可走豁免' -ForegroundColor Yellow
}

# ---------- 5. 保护域触碰提示（pom / sql / application*.yml 不可豁免） ----------
$protected = $changed | Where-Object { $_ -match '(^|/)pom\.xml$' -or $_ -match '^sql/' -or $_ -match '(^|/)application[^/]*\.(yml|yaml|properties)$' }
if ($protected.Count -gt 0) {
    Write-Host "[protected] 触碰保护域（必须携带 OpenSpec change，豁免无效）:" -ForegroundColor Red
    $protected | ForEach-Object { Write-Host "              $_" }
}

# ---------- 6. 本地 hook 安装状态（保护域拦截前移——避免只在 CI 才反馈） ----------
$hookPath = '.git/hooks/pre-commit'
if (Test-Path $hookPath) {
    Write-Host "[hooks]     已安装 $hookPath"
} else {
    Write-Host '[hooks]     未安装 pre-commit hook —— 保护域拦截仍只在 CI发生（滞后）。建议：bash scripts/install-hooks.sh' -ForegroundColor Yellow
}

# ---------- 7. 后续动作提醒 ----------
Write-Host '[reminder] 提交前质量门禁:  bash scripts/code-quality-guard.sh （pre-commit hook 需跑 scripts/install-hooks.sh 安装）'
Write-Host '[reminder] 禅道 MCP 前置:   powershell -ExecutionPolicy Bypass -File .\bin\ensure_zentao_mcp_token.ps1 -ConfigPath .\MCP.json'
Write-Host '[reminder] PR 标题规范:     <type>(<scope>)?: <subject>  （CI 已强制）'
Write-Host '=== preflight done ===' -ForegroundColor Cyan
