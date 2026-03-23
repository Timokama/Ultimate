@echo off
REM Export local PostgreSQL database to SQL file
REM This script exports the ultimate_driving_school database to ultimate_db.sql

echo Exporting local database...
SET PGPASSWORD=postgres

REM Create the export file with schema and data
pg_dump -U postgres -h localhost -d ultimate_driving_school -f ultimate_db.sql --clean --if-exists --inserts --column-inserts

if %ERRORLEVEL% EQU 0 (
    echo Database exported successfully to ultimate_db.sql
    echo.
    echo To import to Render:
    echo 1. Upload ultimate_db.sql to your GitHub repo
    echo 2. In Render dashboard, open a shell to your PostgreSQL database
    echo 3. Run: psql -U your_username -d your_database < ultimate_db.sql
) else (
    echo Export failed. Make sure PostgreSQL is running and you have the correct password.
)

pause
