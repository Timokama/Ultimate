@echo off
REM Ultimate Defensive Driving School - Run Server Script

echo ===========================================
echo Ultimate Defensive Driving School
echo Server Startup Script
echo ===========================================

REM Change to project directory
cd /d "%~dp0"

REM Check if classes exist
echo Checking for compiled classes...

if not exist "UltimateServer.class" (
    echo Error: Server not compiled!
    echo Please run compile.bat first.
    pause
    exit /b 1
)

echo All classes found!

REM Start the server
echo.
echo ===========================================
echo Server Features:
echo - Auto-creates database tables if not exist
echo - Auto-creates admin user if not exist
echo - Auto-inserts sample courses if not exist
echo ===========================================
echo.
echo Starting server...
echo Server will run on: http://localhost:8080
echo Admin login: admin@ultimate.edu / admin123
echo.
echo Press Ctrl+C to stop the server.
echo ===========================================
echo.

java -cp ".;bin;postgresql-42.7.8.jar" UltimateServer

echo.
echo Server stopped.
echo.
pause
