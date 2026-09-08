#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

if [[ -z "${JAVA_HOME:-}" ]]; then
  for candidate in \
    "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" \
    "/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"; do
    if [[ -x "$candidate/bin/java" ]]; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi

if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "==> Check Java and Maven"
java -version
mvn -version

echo "==> SQL dual-track sync checks"
declare -a SQL_PAIRS=(
  "database/migrations/001_core_schema.sql|backend/mes-server/src/main/resources/db/migration/V1__core_schema.sql"
  "database/migrations/002_mes_business_schema.sql|backend/mes-server/src/main/resources/db/migration/V2__mes_business_schema.sql"
  "database/migrations/003_agent_schema.sql|backend/mes-server/src/main/resources/db/migration/V3__agent_schema.sql"
  "database/seeds/001_seed_basic_data.sql|backend/mes-server/src/main/resources/db/seed/R__seed_basic_data.sql"
)

for pair in "${SQL_PAIRS[@]}"; do
  standalone="${pair%%|*}"
  flyway="${pair##*|}"
  standalone_path="$ROOT_DIR/$standalone"
  flyway_path="$ROOT_DIR/$flyway"
  test -f "$standalone_path"
  test -f "$flyway_path"
  if ! cmp -s "$standalone_path" "$flyway_path"; then
    echo "SQL files are out of sync:"
    echo "  $standalone_path"
    echo "  $flyway_path"
    exit 1
  fi
done

echo "==> Backend tests"
(cd "$ROOT_DIR/backend/mes-server" && mvn test)

echo "==> Frontend dependencies"
if [[ ! -d "$ROOT_DIR/frontend/web-admin/node_modules" ]]; then
  (cd "$ROOT_DIR/frontend/web-admin" && npm ci)
fi

echo "==> Frontend build"
(cd "$ROOT_DIR/frontend/web-admin" && npm run build)

echo "==> Legacy business naming scan"
if rg -n "Intelligent|智检|微缺|缺陷检测|FastAPI|mock_category|camera|RT-DETR|透明包装|AI视觉" \
  "$ROOT_DIR/frontend/web-admin/src" "$ROOT_DIR/backend/mes-server/src"; then
  echo "Found legacy IntelligentDetection naming. Please remove it before merging."
  exit 1
fi

echo "==> Module directory checks"
for module in system factory masterdata process barcode planning inventory production quality andon equipment traceability reporting integration agent; do
  for layer in controller service repository entity dto vo converter enums; do
    test -d "$ROOT_DIR/backend/mes-server/src/main/java/com/yunshu/mes/$module/$layer"
  done
done

for layer in controller service vo; do
  test -d "$ROOT_DIR/backend/mes-server/src/main/java/com/yunshu/mes/dashboard/$layer"
done

for view in system factory master-data process barcode planning inventory production quality andon equipment traceability reporting integration agent dashboard; do
  test -d "$ROOT_DIR/frontend/web-admin/src/views/$view"
done

echo "All local checks passed."
echo
echo "Next steps:"
echo "  1. Backend mock API: cd backend/mes-server && mvn spring-boot:run"
echo "  2. Frontend dev server: cd frontend/web-admin && npm run dev"
echo "  3. Open http://127.0.0.1:5173/dashboard/workbench"
