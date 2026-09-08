# Remove external reference repos that must never live in this workspace.
param([string]$ProjectRoot)

$ErrorActionPreference = "SilentlyContinue"
if (-not $ProjectRoot) {
    $ProjectRoot = Split-Path -Parent $PSScriptRoot
}

$patterns = @("ktg-mes-master", "ktg-mes-ui-master", "ktg-mes-*")
$removed = @()

foreach ($item in Get-ChildItem -Path $ProjectRoot -Directory -Force) {
    $name = $item.Name
    $shouldRemove = $false
    foreach ($pat in $patterns) {
        if ($name -like $pat) { $shouldRemove = $true; break }
    }
    if ($shouldRemove) {
        Remove-Item -LiteralPath $item.FullName -Recurse -Force
        $removed += $name
    }
}

if ($removed.Count -gt 0) {
    Write-Host "Removed external reference folders: $($removed -join ', ')" -ForegroundColor Yellow
}

return $removed
