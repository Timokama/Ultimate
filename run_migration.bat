@echo off
echo Running database migration on production...
echo Adding driving_experience column to applications table...

REM Run migration on remote database
psql -h 35.227.164.209 -p 5432 -U postgres -d ultimate_driving_school -c "ALTER TABLE applications ADD COLUMN IF NOT EXISTS driving_experience VARCHAR(20);"

echo.
echo Migration complete.
pause
