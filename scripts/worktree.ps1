#Requires -Version 5.0
<#
.SYNOPSIS
  Safe Git worktree helper for osh-backend (single repo, parallel-branch dev).

.DESCRIPTION
  osh-backend is ONE git repo with 9 Maven modules. Git worktree works at the
  REPO + BRANCH level (not module level): each worktree is a separate directory
  pinned to one branch, so you can build/hotfix one branch while keeping another
  checked out, without stashing back and forth.

  Linked worktrees are created in a SIBLING dir (default: <repoRoot>-wt), never
  nested inside the repo, so they stay invisible to Git and to GitHub CI
  (pr-check.yml builds from pushed branches/PRs, not from your local worktrees).

  Subcommands:
    add    <branch> [-WtRoot <path>]   create a worktree for a branch (local, or origin/<branch>)
    remove <name|path> [-Force]        remove a worktree; refuses if it has uncommitted changes
    list                               list all worktrees
    prune                              remove STALE worktree RECORDS only (never deletes in-use dirs)

  Safety built in:
    - add refuses an existing target path; handles "branch only on origin" by creating a local tracker
    - remove refuses dirty worktrees unless -Force (protects work in progress)
    - prune only cleans broken admin entries; it does NOT delete directories
    - if you point -WtRoot INSIDE the repo, its path is auto-added to .gitignore

.NOTES
  Messages are ASCII-only so they render correctly under Windows PowerShell 5.1's
  default (non-BOM) script encoding. Run:  powershell -ExecutionPolicy Bypass -File scripts\worktree.ps1 <cmd>
#>
[CmdletBinding()]
param(
  [Parameter(Position = 0)][string]$Command,
  [Parameter(Position = 1)][string]$Target,
  [string]$WtRoot,
  [switch]$Force
)

# Native git exit codes are handled manually via $LASTEXITCODE; do not let PS abort mid-flight.
$ErrorActionPreference = 'Continue'

function Show-Usage {
  @'
worktree.ps1 - safe Git worktree helper (single repo, parallel branches)
Worktrees live in a SIBLING dir (default: <repo>-wt), never nested in the repo.

Usage:
  scripts\worktree.ps1 add    <branch> [-WtRoot <path>]
  scripts\worktree.ps1 remove <name|path> [-Force]
  scripts\worktree.ps1 list
  scripts\worktree.ps1 prune

Examples:
  scripts\worktree.ps1 add qa/20260708
  scripts\worktree.ps1 add release/20260628
  scripts\worktree.ps1 list
  scripts\worktree.ps1 remove release-20260628
'@ | Write-Host
}

if ([string]::IsNullOrWhiteSpace($Command)) { Show-Usage; return }

# --- resolve repo root (forward slashes) ---
$repoRoot = (git rev-parse --show-toplevel 2>$null)
if ($LASTEXITCODE -ne 0 -or -not $repoRoot) { Write-Error "Not inside a git repository."; return }
$repoRoot = ($repoRoot -replace '\\', '/').Trim()

if (-not $WtRoot) { $WtRoot = $repoRoot.TrimEnd('/') + '-wt' }
$WtRoot = $WtRoot -replace '\\', '/'

switch ($Command.ToLower()) {

  'add' {
    if (-not $Target) { Write-Error "Usage: worktree.ps1 add <branch>"; return }

    # best-effort refresh of remote refs; ignore offline failures
    git -C $repoRoot fetch --all --prune 2>$null | Out-Null

    $safe = (($Target -replace '[/\\]+', '-') -replace '^-|-$', '')
    $path = "$WtRoot/$safe"
    if (Test-Path $path) { Write-Error "Target path already exists: $path"; return }

    $local  = (git -C $repoRoot rev-parse --verify --quiet "refs/heads/$Target" 2>$null)
    $remote = (git -C $repoRoot rev-parse --verify --quiet "refs/remotes/origin/$Target" 2>$null)

    if (-not (Test-Path $WtRoot)) { New-Item -ItemType Directory -Path $WtRoot -Force | Out-Null }

    if ($local) {
      Write-Host "[worktree] add $path  (branch $Target)"
      git -C $repoRoot worktree add $path $Target
    }
    elseif ($remote) {
      Write-Host "[worktree] add $path  (new local '$Target' tracking origin/$Target)"
      git -C $repoRoot worktree add -b $Target $path "origin/$Target"
    }
    else {
      Write-Error "Branch not found locally or on origin: $Target"; return
    }
    if ($LASTEXITCODE -ne 0) { Write-Error "git worktree add failed (branch already checked out in another worktree?)."; return }

    # Guard only if the worktree dir sits INSIDE the repo (default sibling is outside -> skipped).
    if ($path.StartsWith("$repoRoot/")) {
      $gi = Join-Path $repoRoot '.gitignore'
      $entry = (Split-Path $WtRoot -Leaf) + '/'
      $has = (Test-Path $gi) -and ((Get-Content $gi -Raw) -match [regex]::Escape($entry))
      if (-not $has) {
        Add-Content -Path $gi -Value "`n# git worktree dir (managed by scripts/worktree.ps1)`n$entry"
        Write-Host "[worktree] added '$entry' to .gitignore"
      }
    }
    Write-Host "[worktree] OK -> cd '$path'"
  }

  'remove' {
    if (-not $Target) { Write-Error "Usage: worktree.ps1 remove <name|path> [-Force]"; return }
    if (Test-Path $Target) { $path = ($Target -replace '\\', '/') } else { $path = "$WtRoot/$Target" }
    if (-not (Test-Path $path)) { Write-Error "Worktree path not found: $path"; return }

    $dirty = (git -C $path status --porcelain 2>$null)
    if ($dirty -and -not $Force) {
      Write-Host "[worktree] refusing: uncommitted changes in $path" -ForegroundColor Yellow
      $dirty | ForEach-Object { Write-Host "   $_" }
      Write-Host "[worktree] commit/stash first, or pass -Force to discard." -ForegroundColor Yellow
      return
    }
    if ($Force) { git -C $repoRoot worktree remove --force $path } else { git -C $repoRoot worktree remove $path }
    if ($LASTEXITCODE -ne 0) { Write-Error "git worktree remove failed."; return }
    Write-Host "[worktree] removed $path"
  }

  'list' {
    git -C $repoRoot worktree list
  }

  'prune' {
    Write-Host "[worktree] pruning STALE records only (in-use directories are NOT deleted)"
    git -C $repoRoot worktree prune -v
  }

  default { Show-Usage }
}
