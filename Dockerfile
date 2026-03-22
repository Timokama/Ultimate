# Use official OpenJDK runtime as base image
FROM eclipse-temurin:17-jdk-slim

# Set working directory
WORKDIR /app

# Install curl for downloading PostgreSQL driver
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy all Java source files
COPY *.java ./

# Download PostgreSQL JDBC driver
RUN curl -L -o postgresql-42.7.8.jar https://jdbc.postgresql.org/download/postgresql-42.7.8.jar

# Compile Java files
RUN javac -sourcepath . -cp ".:postgresql-42.7.8.jar" *.java

# Expose port 8080
EXPOSE 8080

# Run the Java application
CMD ["java", "UltimateServer"]
