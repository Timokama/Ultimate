@echo off
echo Running database migration...
psql -U postgres -d postgres -c "ALTER TABLE applications ALTER COLUMN preferred_schedule TYPE VARCHAR(50);"
echo Migration complete.
pause
