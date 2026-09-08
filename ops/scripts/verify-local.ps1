$ErrorActionPreference = "Stop"

$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..\..")

if (-not $env:JAVA_HOME) {
  Write-Host "JAVA_HOME is not set. Install JDK 17 and set JAVA_HOME if Java is unavailable."
  Write-Host "Example: `$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'"
}

Write-Host "==> Check Java and Maven"
java -version
mvn -version

Write-Host "==> SQL dual-track sync checks"
$SqlPairs = @(
  @{
    Standalone = Join-Path $RootDir "database\migrations\001_core_schema.sql"
    Flyway = Join-Path $RootDir "backend\mes-server\src\main\resources\db\migration\V1__core_schema.sql"
  },
  @{
    Standalone = Join-Path $RootDir "database\migrations\002_mes_business_schema.sql"
    Flyway = Join-Path $RootDir "backend\mes-server\src\main\resources\db\migration\V2__mes_business_schema.sql"
  },
  @{
    Standalone = Join-Path $RootDir "database\migrations\003_agent_schema.sql"
    Flyway = Join-Path $RootDir "backend\mes-server\src\main\resources\db\migration\V3__agent_schema.sql"
  },
  @{
    Standalone = Join-Path $RootDir "database\seeds\001_seed_basic_data.sql"
    Flyway = Join-Path $RootDir "backend\mes-server\src\main\resources\db\seed\R__seed_basic_data.sql"
  }
)

foreach ($Pair in $SqlPairs) {
  if (-not (Test-Path $Pair.Standalone)) {
    throw "Missing standalone SQL file: $($Pair.Standalone)"
  }
  if (-not (Test-Path $Pair.Flyway)) {
    throw "Missing Flyway SQL file: $($Pair.Flyway)"
  }
  $StandaloneHash = (Get-FileHash $Pair.Standalone -Algorithm SHA256).Hash
  $FlywayHash = (Get-FileHash $Pair.Flyway -Algorithm SHA256).Hash
  if ($StandaloneHash -ne $FlywayHash) {
    throw "SQL files are out of sync:`n  $($Pair.Standalone)`n  $($Pair.Flyway)"
  }
}

Write-Host "==> Backend tests"
Push-Location (Join-Path $RootDir "backend\mes-server")
mvn test
Pop-Location

Write-Host "==> Frontend dependencies"
$WebAdminDir = Join-Path $RootDir "frontend\web-admin"
if (-not (Test-Path (Join-Path $WebAdminDir "node_modules"))) {
  Push-Location $WebAdminDir
  npm ci
  Pop-Location
}

Write-Host "==> Frontend build"
Push-Location $WebAdminDir
npm run build
Pop-Location

Write-Host "==> Legacy business naming scan"
$LegacyPattern = "Intelligent|智检|微缺|缺陷检测|FastAPI|mock_category|camera|RT-DETR|透明包装|AI视觉"
rg -n $LegacyPattern `
  (Join-Path $RootDir "frontend\web-admin\src") `
  (Join-Path $RootDir "backend\mes-server\src")
if ($LASTEXITCODE -eq 0) {
  throw "Found legacy IntelligentDetection naming. Please remove it before merging."
}
if ($LASTEXITCODE -ne 1) {
  throw "Legacy naming scan failed with exit code $LASTEXITCODE."
}

Write-Host "==> Module directory checks"
$BackendModules = @("system", "factory", "masterdata", "process", "barcode", "planning", "inventory", "production", "quality", "andon", "equipment", "traceability", "reporting", "integration", "agent")
$BackendLayers = @("controller", "service", "repository", "entity", "dto", "vo", "converter", "enums")
foreach ($Module in $BackendModules) {
  foreach ($Layer in $BackendLayers) {
    $Path = Join-Path $RootDir "backend\mes-server\src\main\java\com\yunshu\mes\$Module\$Layer"
    if (-not (Test-Path $Path)) {
      throw "Missing backend directory: $Path"
    }
  }
}

$DashboardLayers = @("controller", "service", "vo")
foreach ($Layer in $DashboardLayers) {
  $Path = Join-Path $RootDir "backend\mes-server\src\main\java\com\yunshu\mes\dashboard\$Layer"
  if (-not (Test-Path $Path)) {
    throw "Missing dashboard directory: $Path"
  }
}

$FrontendViews = @("system", "factory", "master-data", "process", "barcode", "planning", "inventory", "production", "quality", "andon", "equipment", "traceability", "reporting", "integration", "agent", "dashboard")
foreach ($View in $FrontendViews) {
  $Path = Join-Path $RootDir "frontend\web-admin\src\views\$View"
  if (-not (Test-Path $Path)) {
    throw "Missing frontend view directory: $Path"
  }
}

Write-Host "All local checks passed."
Write-Host ""
Write-Host "Next steps on Windows:"
Write-Host "  1. Backend mock API: cd backend\mes-server; mvn spring-boot:run"
Write-Host "  2. Frontend dev server: cd frontend\web-admin; npm run dev"
Write-Host "  3. Open http://127.0.0.1:5173/dashboard/workbench"
