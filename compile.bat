@echo off
REM Ultimate Defensive Driving School - Compilation Script

echo ===========================================
echo Ultimate Defensive Driving School
echo Compiling Java files...
echo ===========================================

REM Change to project directory
cd /d "%~dp0"

echo Current directory: %CD%
echo.

REM Check if PostgreSQL driver exists
if not exist "postgresql-42.7.8.jar" (
    echo Error: PostgreSQL driver not found!
    echo Please download postgresql-42.7.8.jar and place it in the Ultimate directory.
    echo Download from: https://jdbc.postgresql.org/download/postgresql-42.7.8.jar
    pause
    exit /b 1
)

REM Delete old class files
echo Deleting old class files...
del *.class 2>nul
if exist "out" rd /s /q out

REM Create output directory
mkdir out

REM Compile Java files to out directory using sourcepath
echo Compiling Java classes...
javac -sourcepath . -cp ".;postgresql-42.7.8.jar" -d out *.java 2>&1

if %errorlevel% equ 0 (
    echo.
    echo Compilation successful!
    echo.
    echo Copying class files to current directory...
    copy out\*.class . >nul
    echo.
    dir /b *.class
    echo.
    echo To run the server, execute: run.bat
    echo.
) else (
    echo.
    echo Compilation failed!
    echo Please check the errors above.
    echo.
    echo Alternative: Run the commands from COMPILE_MANUAL.txt manually
    pause
    exit /b 1
)

pause
