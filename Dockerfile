# Use official OpenJDK runtime as base image
FROM amazoncorretto:17

# Set working directory
WORKDIR /app

# Install curl for downloading PostgreSQL driver
RUN yum install -y curl && yum clean all

# Copy all Java source files
COPY *.java ./

# Copy web directory for static files
COPY web/ ./web/

# Download PostgreSQL JDBC driver
RUN curl -L -o postgresql-42.7.8.jar https://jdbc.postgresql.org/download/postgresql-42.7.8.jar

# Compile Java files
RUN javac -sourcepath . -cp ".:postgresql-42.7.8.jar" *.java

# Expose port 8080
EXPOSE 8080

# Run the Java application with PostgreSQL driver in classpath
CMD ["java", "-cp", ".:postgresql-42.7.8.jar", "UltimateServer"]
