# Ultimate Defensive Driving School - Java Project

A complete web application for managing driving school operations including student applications, course management, staff management, and M-Pesa payment integration.

## Features

- **User Authentication**: JWT-based authentication for admins, staff, and applicants
- **Course Management**: Manage driving and computer courses
- **Application Processing**: Handle student applications with status tracking
- **Staff Management**: Assign staff to different branches/locations
- **Payment Processing**: M-Pesa message parsing and fee tracking
- **Contact Form**: Customer inquiry management
- **Responsive Design**: Works on desktop and mobile devices

## Project Structure

```
Java_project/Ultimate/
├── ApiHandler.java          # Base HTTP handler with common utilities
├── ApplicationHandler.java   # Application submission and management
├── AuthHandler.java         # Authentication (login/register)
├── ContactHandler.java      # Contact form handling
├── CourseHandler.java       # Course CRUD operations
├── DBConnection.java        # Database connection and operations
├── JWTUtil.java            # JWT token utilities
├── StaffHandler.java        # Staff dashboard operations
├── StaticHandler.java       # Static file serving
├── UltimateServer.java      # Main server entry point
├── UserHandler.java         # User management
├── database.sql             # Database schema and sample data
├── postgresql-42.7.8.jar    # PostgreSQL JDBC driver
├── compile.bat              # Windows compilation script
├── run.bat                  # Windows run script
├── PLAN.md                  # Implementation plan
└── web/
    ├── index.html           # Home page
    ├── login.html           # Login page
    ├── register.html        # Registration page
    ├── courses.html         # Course listing
    ├── apply.html           # Application form
    ├── dashboard.html       # User dashboard
    ├── admin.html           # Admin dashboard
    ├── staff.html           # Staff dashboard
    ├── js/
    │   └── app.js           # Frontend JavaScript
    ├── css/
    │   └── styles.css       # Main stylesheet
    └── logo/
        └── UDDS LOGO.jpg   # School logo
```

## Prerequisites

1. **Java Development Kit (JDK)** - Version 8 or higher
2. **PostgreSQL** - Version 12 or higher
3. **PostgreSQL JDBC Driver** - Included (postgresql-42.7.8.jar)

## Database Setup

### Option 1: Fresh Installation

1. Create a new PostgreSQL database:
   ```bash
   psql -U postgres -c "CREATE DATABASE ultimate_driving_school;"
   ```

2. Run the database schema and sample data:
   ```bash
   psql -U postgres -d ultimate_driving_school -f database.sql
   ```

### Option 2: Using pgAdmin

1. Open pgAdmin and create a new database named `ultimate_driving_school`
2. Open the Query Tool and run the contents of `database.sql`

### Default Login Credentials

After running `database.sql`, the following users are created:

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@ultimate.edu | admin123 |
| Staff | timo@gmail.com | staff123 |
| Applicant | timo.munyiri@gmail.com | student123 |

## Compilation

### Windows

```bash
cd Java_project/Ultimate
compile.bat
```

### Manual Compilation

```bash
cd Java_project/Ultimate
javac -cp ".;postgresql-42.7.8.jar" *.java
```

## Running the Server

### Windows

```bash
run.bat
```

### Manual

```bash
cd Java_project/Ultimate
java -cp ".;postgresql-42.7.8.jar" UltimateServer
```

## Accessing the Application

Once the server is running:

- **Local**: http://localhost:8080
- **Network**: http://YOUR_IP:8080 (find your IP in server output)

### Pages

| URL | Description | Access |
|-----|-------------|--------|
| / | Home page | Public |
| /login.html | Login page | Public |
| /register.html | Registration | Public |
| /courses.html | Course listing | Public |
| /apply.html | Application form | Public |
| /dashboard.html | User dashboard | Authenticated users |
| /admin.html | Admin dashboard | Admins only |
| /staff.html | Staff dashboard | Staff only |

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/auth/register | Register new user |
| POST | /api/auth/login | Login user |
| POST | /api/auth/logout | Logout user |
| GET | /api/auth/me | Get current user |

### Applications

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/applications | Submit application |
| GET | /api/applications | Get user's applications |
| GET | /api/applications/all | Get all applications (admin) |
| PUT | /api/applications/{id}/status | Update status (admin) |

### Courses

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/courses | Get all courses |
| GET | /api/courses/{id} | Get course by ID |
| POST | /api/courses | Create course (admin) |
| PUT | /api/courses/{id} | Update course (admin) |
| DELETE | /api/courses/{id} | Delete course (admin) |

### Staff

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/staff/locations | Get all locations |
| GET | /api/staff/location-students | Get students by location |
| PUT | /api/staff/application/fees | Update fees |
| PUT | /api/staff/application/location | Update location |
| GET | /api/staff/all | Get all staff (admin) |
| POST | /api/staff/create | Create staff (admin) |

### Contact

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/contact | Submit contact form |
| GET | /api/contacts | Get all messages (admin) |
| PUT | /api/contacts/{id} | Update message status |
| DELETE | /api/contacts/{id} | Delete message |

## Column Naming Convention

This project uses consistent column naming:

- **`users.location`**: Staff member's assigned branch/location
- **`applications.training_location`**: Where the applicant will receive training

**Note**: The `applications` table previously had both `location` and `training_location` columns, which caused confusion. The schema has been updated to use only `training_location` for applications.

## Troubleshooting

### Database Connection Failed

1. Ensure PostgreSQL is running
2. Check credentials in `DBConnection.java`:
   ```java
   private static final String URL = "jdbc:postgresql://localhost:5432/ultimate_driving_school";
   private static final String USER = "postgres";
   private static final String PASSWORD = "secret123";
   ```

3. Update the password to match your PostgreSQL setup

### Port 8080 Already in Use

Edit `UltimateServer.java` and change the port:
```java
private static final int PORT = 8080; // Change to another port
```

### Compilation Errors

1. Ensure JDK is installed correctly
2. Verify `postgresql-42.7.8.jar` is in the same directory
3. Clean and recompile:
   ```bash
   del *.class
   javac -cp ".;postgresql-42.7.8.jar" *.java
   ```

### JWT Token Issues

Tokens expire after 24 hours. Users will need to re-login after expiration.

## Database Backup and Restore

### Backup

```bash
pg_dump -U postgres -d ultimate_driving_school > backup.sql
```

### Restore

```bash
psql -U postgres -d ultimate_driving_school -f backup.sql
```

## Technology Stack

- **Backend**: Java HttpServer (built-in)
- **Database**: PostgreSQL
- **JDBC**: PostgreSQL JDBC Driver
- **Authentication**: JWT (JSON Web Tokens)
- **Frontend**: HTML5, CSS3, JavaScript (ES6)
- **Icons**: Font Awesome

## License

This project is for educational purposes.
