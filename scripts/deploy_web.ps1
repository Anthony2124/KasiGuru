# deploy_web.ps1 — deploys the three Vercel web portals from the repo.
#
# Usage:
#   .\scripts\deploy_web.ps1
#
# Requirements:
#   - Vercel CLI installed (npm install -g vercel)
#   - Authentication via ONE of:
#       1) a logged-in session (run `vercel login` once), or
#       2) $env:VERCEL_TOKEN set, or
#       3) a token file at C:\KasiGuru\.vercel_token (gitignored; keep private)
#
# Deploys:
#   admin-website/           -> kasi-guru project   (restricted-area placeholder)
#   admin-website/admin/     -> admin project       (login + dashboard)
#   admin-website/download/  -> download project    (public APK page)

$ErrorActionPreference = 'Continue'
# Prevent native stderr (e.g. the vercel CLI banner) from becoming terminating
# errors when output is piped/redirected (PowerShell 7.3+).
$PSNativeCommandUseErrorActionPreference = $false
$repoRoot = Split-Path -Parent $PSScriptRoot

if (-not (Get-Command vercel -ErrorAction SilentlyContinue)) {
    Write-Error "Vercel CLI not found. Install it with: npm install -g vercel"
}

if (-not $env:VERCEL_TOKEN) {
    $tokenFile = 'C:\KasiGuru\.vercel_token'
    if (Test-Path $tokenFile) {
        $env:VERCEL_TOKEN = (Get-Content $tokenFile -Raw).Trim()
    }
}

$projects = @(
    @{ Name = 'kasi-guru (root placeholder)'; Path = Join-Path $repoRoot 'admin-website';       VercelProject = 'kasi-guru' },
    @{ Name = 'admin (login + dashboard)';    Path = Join-Path $repoRoot 'admin-website\admin';   VercelProject = 'admin' },
    @{ Name = 'download (public APK page)';   Path = Join-Path $repoRoot 'admin-website\download'; VercelProject = 'download' }
)

foreach ($p in $projects) {
    Write-Host ""
    Write-Host ("=== Deploying " + $p.Name + " ===") -ForegroundColor Cyan

    Push-Location $p.Path
    try {
        # Ensure the folder is linked to the right Vercel project (auto-link on fresh clones).
        if (-not (Test-Path (Join-Path $p.Path '.vercel'))) {
            vercel link --yes --project $p.VercelProject --scope 'adrianmiras13-8259s-projects'
            if ($LASTEXITCODE -ne 0) { throw "vercel link failed in $($p.Path)" }
        }

        vercel --prod --yes
        if ($LASTEXITCODE -ne 0) { throw "vercel --prod failed in $($p.Path)" }
        Write-Host ("OK - " + $p.Name + " deployed") -ForegroundColor Green
    } finally {
        Pop-Location
    }
}

Write-Host ""
Write-Host "All three portals deployed." -ForegroundColor Green
