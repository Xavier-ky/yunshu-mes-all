# 构建 UReport3 并安装到本地 Maven 仓库（首次集成 mes-server 前运行一次）
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $root
$mvnw = Join-Path $repoRoot "backend\mes-server\mvnw.cmd"
$ureportRoot = Join-Path $repoRoot "third-party\ureport3"

if (-not (Test-Path $ureportRoot)) {
    Write-Host "Cloning UReport3..."
    git -c http.proxy= -c https.proxy= clone --depth 1 https://gitee.com/haron_1_0/ureport3.git $ureportRoot
}

Write-Host "=== ureport3-parent ===" -ForegroundColor Cyan
& $mvnw -f (Join-Path $ureportRoot "ureport3-parent\pom.xml") install "-DskipTests" -q
if ($LASTEXITCODE -ne 0) { throw "Build failed: ureport3-parent" }

Write-Host "=== ureport3 reactor (core -> font -> console) ===" -ForegroundColor Cyan
& $mvnw -f (Join-Path $ureportRoot "pom.xml") clean install "-DskipTests" -q
if ($LASTEXITCODE -ne 0) { throw "Build failed: ureport3 reactor" }

Write-Host "UReport3 3.0.1 installed to local Maven repository." -ForegroundColor Green
