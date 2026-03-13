param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$userHome = if ($env:USERPROFILE) {
    $env:USERPROFILE
} else {
    [Environment]::GetFolderPath("UserProfile")
}

$gradleUserHome = Join-Path $repoRoot ".gradle-user"
$androidUserHome = Join-Path $repoRoot ".android-user"
$kotlinDaemonDir = Join-Path $repoRoot ".kotlin-daemon"
$androidPrefsDir = Join-Path $userHome ".android"
$analyticsSettings = Join-Path $androidPrefsDir "analytics.settings"

New-Item -ItemType Directory -Force -Path $gradleUserHome | Out-Null
New-Item -ItemType Directory -Force -Path $androidUserHome | Out-Null
New-Item -ItemType Directory -Force -Path $kotlinDaemonDir | Out-Null
New-Item -ItemType Directory -Force -Path $androidPrefsDir | Out-Null

if (-not (Test-Path $analyticsSettings)) {
    New-Item -ItemType File -Path $analyticsSettings | Out-Null
}

$env:GRADLE_USER_HOME = $gradleUserHome
$env:ANDROID_USER_HOME = $androidUserHome
$env:KOTLIN_DAEMON_DIR = $kotlinDaemonDir

if (-not $env:HOME) {
    $env:HOME = $userHome
}

Push-Location $repoRoot
try {
    & (Join-Path $repoRoot "gradlew.bat") @GradleArgs
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
