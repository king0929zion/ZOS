#requires -version 5.0
Param(
    [string]$Message = "chore: sync workspace"
)

function Invoke-Git {
    param([string[]]$Args)
    $process = Start-Process -FilePath "git" -ArgumentList $Args -NoNewWindow -PassThru -Wait
    if ($process.ExitCode -ne 0) {
        throw "git $Args failed with exit code $($process.ExitCode)"
    }
}

if (-not (Test-Path ".git")) {
    Write-Host "Initializing git repository..."
    Invoke-Git @("init")
}

try {
    Invoke-Git @("remote", "remove", "origin") | Out-Null
} catch {
    # ignore if remote doesn't exist
}
Invoke-Git @("remote", "add", "origin", "https://github.com/king0929zion/ZOS")

$currentBranch = (git branch --show-current).Trim()
if ($currentBranch -ne "main") {
    Write-Host "Switching current branch '$currentBranch' to 'main'..."
    Invoke-Git @("branch", "-M", "main")
}

Invoke-Git @("add", "--all")

$status = git status --porcelain
if (-not $status) {
    Write-Host "No changes to commit. Skipping commit step."
} else {
    Invoke-Git @("commit", "-m", $Message)
}

Invoke-Git @("push", "-u", "origin", "main")

Write-Host "Push to https://github.com/king0929zion/ZOS completed."
