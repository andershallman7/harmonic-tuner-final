param(
    [switch]$SkipCompilation,
    [switch]$UseBundledJava
)

# Harmonic Tuner - PowerShell Launcher
# Run this script to start the tuner

Write-Host "`n================================================" -ForegroundColor Cyan
Write-Host "           Harmonic Tuner - Launcher" -ForegroundColor Cyan
Write-Host "================================================`n" -ForegroundColor Cyan

# Get the directory where this script is located
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$projectDir = $scriptDir
$srcDir = Join-Path $projectDir "src"

# Change to project directory
Set-Location $projectDir

# Check for Java
Write-Host "Checking for Java..." -ForegroundColor Yellow
$javaExe = if (Test-Path "java-8\bin\java.exe") {
    "java-8\bin\java.exe"
} else {
    "java"
}

& $javaExe -version 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "`nERROR: Java not found!" -ForegroundColor Red
    Write-Host "Please install Java 8 or higher from: https://www.oracle.com/java/technologies/javase-downloads.html`n" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "Java found!`n" -ForegroundColor Green

# Compile if not skipped
if (-not $SkipCompilation) {
    Write-Host "Compiling Java files..." -ForegroundColor Yellow
    Set-Location $srcDir
    & javac com\harmonic\tuner\*.java
    if ($LASTEXITCODE -ne 0) {
        Write-Host "`nERROR: Compilation failed!`n" -ForegroundColor Red
        Read-Host "Press Enter to exit"
        exit 1
    }
    Write-Host "Compilation successful!`n" -ForegroundColor Green
} else {
    Write-Host "Skipping compilation...`n" -ForegroundColor Yellow
}

# Run the tuner
Write-Host "Starting Harmonic Tuner...`n" -ForegroundColor Cyan
& java com.harmonic.tuner.Main

if ($LASTEXITCODE -ne 0) {
    Write-Host "`nERROR: Failed to run the tuner!`n" -ForegroundColor Red
}

Write-Host "`nTuner closed." -ForegroundColor Yellow
Read-Host "Press Enter to exit"