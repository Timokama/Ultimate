@echo off
REM Export all data from local PostgreSQL database
REM This exports your actual data to ultimate_db_data.sql

echo Exporting data from ultimate_driving_school...

REM Export data only (no schema) with INSERT statements
pg_dump -U postgres -h localhost -d ultimate_driving_school --data-only --inserts -f ultimate_db_data.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Data exported successfully to ultimate_db_data.sql
    echo File size:
    dir ultimate_db_data.sql
) else (
    echo Export failed!
)

pause
