#!/bin/bash
# Build script for Ultimate Defensive Driving School

echo "Compiling Java files..."

# Download PostgreSQL driver if not exists
if [ ! -f "postgresql-42.7.8.jar" ]; then
    echo "Downloading PostgreSQL driver..."
    curl -L -o postgresql-42.7.8.jar https://jdbc.postgresql.org/download/postgresql-42.7.8.jar
fi

# Compile Java files
javac -sourcepath . -cp ".:postgresql-42.7.8.jar" *.java

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
else
    echo "Compilation failed!"
    exit 1
fi
