@echo off
echo Running database migration on Render production...
echo Adding missing columns to enrollments table...

REM Run migration on Render database using the connection from render.yaml
REM DATABASE_URL: postgresql://ultimate_driving_school_user:GUF6IzMPtUV7Atn5mUyzZYqIHVfzOp1Y@dpg-d6vpp01r0fns73cei38g-a/ultimate_driving_school_278c

set PGHOST=dpg-d6vpp01r0fns73cei38g-a.oregon-postgres.render.com
set PGPORT=5432
set PGDATABASE=ultimate_driving_school_278c
set PGUSER=ultimate_driving_school_user
set PGPASSWORD=GUF6IzMPtUV7Atn5mUyzZYqIHVfzOp1Y

echo Adding location_id column...
set PGPASSWORD=GUF6IzMPtUV7Atn5mUyzZYqIHVfzOp1Y psql -h %PGHOST% -p %PGPORT% -d %PGDATABASE% -U %PGUSER% -c "ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS location_id INTEGER;"

echo Adding license_type column...
set PGPASSWORD=GUF6IzMPtUV7Atn5mUyzZYqIHVfzOp1Y psql -h %PGHOST% -p %PGPORT% -d %PGDATABASE% -U %PGUSER% -c "ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS license_type VARCHAR(50);"

echo Adding driving_course column...
set PGPASSWORD=GUF6IzMPtUV7Atn5mUyzZYqIHVfzOp1Y psql -h %PGHOST% -p %PGPORT% -d %PGDATABASE% -U %PGUSER% -c "ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS driving_course VARCHAR(100);"

echo Adding computer_course column...
set PGPASSWORD=GUF6IzMPtUV7Atn5mUyzZYqIHVfzOp1Y psql -h %PGHOST% -p %PGPORT% -d %PGDATABASE% -U %PGUSER% -c "ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS computer_course VARCHAR(100);"

echo Adding transmission column...
set PGPASSWORD=GUF6IzMPtUV7Atn5mUyzZYqIHVfzOp1Y psql -h %PGHOST% -p %PGPORT% -d %PGDATABASE% -U %PGUSER% -c "ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS transmission VARCHAR(50);"

echo Adding preferred_schedule column...
set PGPASSWORD=GUF6IzMPtUV7Atn5mUyzZYqIHVfzOp1Y psql -h %PGHOST% -p %PGPORT% -d %PGDATABASE% -U %PGUSER% -c "ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS preferred_schedule VARCHAR(50);"

echo Adding training_location column...
set PGPASSWORD=GUF6IzMPtUV7Atn5mUyzZYqIHVfzOp1Y psql -h %PGHOST% -p %PGPORT% -d %PGDATABASE% -U %PGUSER% -c "ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS training_location VARCHAR(200);"

echo Adding driving_experience to applications table...
set PGPASSWORD=GUF6IzMPtUV7Atn5mUyzZYqIHVfzOp1Y psql -h %PGHOST% -p %PGPORT% -d %PGDATABASE% -U %PGUSER% -c "ALTER TABLE applications ADD COLUMN IF NOT EXISTS driving_experience VARCHAR(20);"

echo.
echo Migration complete.
pause
