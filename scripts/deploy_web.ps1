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
    @{ Name = 'download (public APK page)';   Path = Join-Path $repoRoot 'admin-website\download'; VercelProject = 'download'; RequiresApk = $true }
)

$anyFailed = $false

foreach ($p in $projects) {
    Write-Host ""
    Write-Host ("=== Deploying " + $p.Name + " ===") -ForegroundColor Cyan

    # A manual deploy of the download project with no APK physically present ships a
    # brochure page whose download buttons all 404 — the exact failure mode that took
    # the site offline last time. The release workflow always builds and copies one in
    # first; this only guards the manual path, so refuse rather than silently deploying
    # a broken page.
    if ($p.RequiresApk -and -not (Test-Path (Join-Path $p.Path 'kasiguru-latest.apk'))) {
        Write-Error ("Refusing to deploy " + $p.Name + ": no kasiguru-latest.apk in " + $p.Path + ". Run the tagged release workflow (which builds and copies it in) instead of deploying this manually, or copy a built APK into that folder first if you really mean to.")
        $anyFailed = $true
        continue
    }

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
if ($anyFailed) {
    Write-Host "Done, but at least one portal was skipped or failed — see above." -ForegroundColor Yellow
    exit 1
} else {
    Write-Host "All three portals deployed." -ForegroundColor Green
}
