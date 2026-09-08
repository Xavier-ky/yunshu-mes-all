@echo off
setlocal
chcp 65001 >nul

set "SQL=%~dp0..\backend\mes-server\src\main\resources\db\seed\R__seed_dv_equipment.sql"
set "SQL_STORY=%~dp0..\backend\mes-server\src\main\resources\db\seed\R__seed_dv_equipment_story.sql"
set "MYSQL=mysql"
set "HOST=localhost"
set "PORT=3306"
set "USER=root"
set "PASS=051002sry"
set "DB=fan_mes"

echo Applying dv equipment seed (utf8mb4) ...
"%MYSQL%" -h %HOST% -P %PORT% -u %USER% -p%PASS% --default-character-set=utf8mb4 %DB% < "%SQL%"
if errorlevel 1 (
  echo Base seed failed.
  exit /b 1
)
echo Applying dv equipment story seed ...
"%MYSQL%" -h %HOST% -P %PORT% -u %USER% -p%PASS% --default-character-set=utf8mb4 %DB% < "%SQL_STORY%"
if errorlevel 1 (
  echo Story seed failed.
  exit /b 1
)
echo Done.
exit /b 0
