import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Ultimate Defensive Driving School - Main Server
 * Uses Java HttpServer, JDBC for PostgreSQL, and JWT for authentication
 */
public class UltimateServer {
    
    private static final int PORT = 8080;
    private static final String WEB_ROOT = "web";
    private static final String ADMIN_EMAIL = "admin@ultimate.edu";
    private static final String ADMIN_PASSWORD = "admin123";
    
    public static void main(String[] args) {
        try {
            // Initialize database and create admin user if not exists
            initializeDatabase();
            
            // Create HTTP server (0.0.0.0 for global access)
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
            
            // Create context for static files
            server.createContext("/", new StaticHandler(WEB_ROOT));
            
            // API routes
            server.createContext("/api/auth", new AuthHandler());
            server.createContext("/api/applications", new ApplicationHandler());
            server.createContext("/api/courses", new CourseHandler());
            server.createContext("/api/users", new UserHandler());
            server.createContext("/api/user", new ApplicationHandler());
            server.createContext("/api/contact", new ContactHandler());
            server.createContext("/api/contacts", new ContactHandler());
            server.createContext("/api/locations", new LocationHandler());
            server.createContext("/api/staff", new StaffHandler());
            server.createContext("/api/enrollments", new EnrollmentHandler());
            server.createContext("/api/attendance", new AttendanceHandler());
            server.createContext("/api/mpesa", new MpesaHandler());
            server.createContext("/api/classes", new ClassHandler());
            server.createContext("/api/departments", new DepartmentHandler());
            
            // Set executor (null = synchronous)
            server.setExecutor(null);
            
            // Start server
            server.start();
            
            System.out.println("===========================================");
            System.out.println("Ultimate Defensive Driving School Server");
            System.out.println("===========================================");
            System.out.println("");
            System.out.println("Server URLs:");
            System.out.println("  Local:   http://localhost:8080");
            System.out.println("  Global:  http://0.0.0.0:8080");
            System.out.println("  Your IP: http://" + java.net.InetAddress.getLocalHost().getHostAddress() + ":8080");
            System.out.println("");
            System.out.println("Available endpoints:");
            System.out.println("  GET  /                    - Home page");
            System.out.println("  GET  /courses.html        - Courses page");
            System.out.println("  GET  /apply.html          - Application form");
            System.out.println("  GET  /login.html          - Login page");
            System.out.println("  GET  /dashboard.html      - User dashboard");
            System.out.println("  GET  /admin.html          - Admin dashboard");
            System.out.println("");
            System.out.println("API Endpoints:");
            System.out.println("  POST /api/auth/register      - Register new user");
            System.out.println("  POST /api/auth/login       - Login user");
            System.out.println("  POST /api/auth/logout      - Logout user");
            System.out.println("  GET  /api/auth/me         - Get current user");
            System.out.println("  POST /api/auth/create-admin - Create admin user (secret key required)");
            System.out.println("  GET  /api/courses         - Get all courses");
            System.out.println("  GET  /api/courses/{id}    - Get course by ID");
            System.out.println("  POST /api/courses         - Create course (admin)");
            System.out.println("  PUT  /api/courses/{id}    - Update course (admin)");
            System.out.println("  DELETE /api/courses/{id}  - Delete course (admin)");
            System.out.println("  GET  /api/users           - Get all users (admin)");
            System.out.println("  PUT  /api/users/{id}      - Update user role (admin)");
            System.out.println("  DELETE /api/users/{id}    - Delete user (admin)");
            System.out.println("  POST /api/applications     - Submit application");
            System.out.println("  GET  /api/applications    - Get my applications");
            System.out.println("  GET  /api/applications/all - Get all applications (admin)");
            System.out.println("  PUT  /api/applications/{id}/status - Update status (admin)");
            System.out.println("  POST /api/contact         - Submit contact form (public)");
            System.out.println("  GET  /api/contacts        - Get all messages (admin)");
            System.out.println("  PUT  /api/contacts/{id}    - Update message status (admin)");
            System.out.println("  DELETE /api/contacts/{id}  - Delete message (admin)");
            System.out.println("  GET  /api/staff/location-students - Get students by location (staff)");
            System.out.println("  GET  /api/staff/fees-summary     - Get fees summary (staff)");
            System.out.println("  PUT  /api/staff/application/fees - Update application fees (staff)");
            System.out.println("  PUT  /api/staff/profile         - Update staff profile (staff)");
            System.out.println("  POST /api/enrollments        - Create enrollment (admin/staff)");
            System.out.println("  GET  /api/enrollments        - List all enrollments (admin/staff)");
            System.out.println("  GET  /api/enrollments/{id}   - Get enrollment by ID (admin/staff/student)");
            System.out.println("  PUT  /api/enrollments/{id}   - Update enrollment (admin)");
            System.out.println("  DELETE /api/enrollments/{id}  - Delete enrollment (admin)");
            System.out.println("  GET  /api/students/{id}/enrollments - Get student's enrollments");
            System.out.println("  POST /api/attendance        - Create attendance record (admin/staff)");
            System.out.println("  GET  /api/attendance        - List all attendance records (admin/staff)");
            System.out.println("  GET  /api/attendance/{id}   - Get attendance record by ID");
            System.out.println("  PUT  /api/attendance/{id}   - Update attendance record (admin/staff)");
            System.out.println("  DELETE /api/attendance/{id} - Delete attendance record (admin)");
            System.out.println("  GET  /api/attendance/class/{classId} - Get class attendance (admin/staff)");
            System.out.println("  GET  /api/attendance/student/{studentId} - Get student's attendance");
            System.out.println("  POST /api/mpesa/stkpush - Initiate M-Pesa STK Push (admin/staff)");
            System.out.println("  POST /api/mpesa/callback - M-Pesa payment callback (public)");
            System.out.println("  GET  /api/mpesa/transactions - List all transactions (admin/staff)");
            System.out.println("  GET  /api/mpesa/transactions/{id} - Get transaction by ID");
            System.out.println("  GET  /api/mpesa/transactions/phone/{phone} - Get transactions by phone");
            System.out.println("  POST /api/mpesa/verify - Verify transaction status (admin/staff)");
            System.out.println("");
            System.out.println("Admin Login:");
            System.out.println("  Email: " + ADMIN_EMAIL);
            System.out.println("  Password: " + ADMIN_PASSWORD);
            System.out.println("");
            
            // Display network access information
            System.out.println("===========================================");
            System.out.println("ACCESS URLs:");
            System.out.println("===========================================");
            System.out.println("Local access:  http://localhost:" + PORT);
            System.out.println("Network access: http://" + getLocalIpAddress() + ":" + PORT);
            System.out.println("");
            System.out.println("Note: Make sure your firewall allows port " + PORT);
            System.out.println("For internet access, configure port forwarding on your router");
            System.out.println("===========================================");
            System.out.println("");
            System.out.println("Press Ctrl+C to stop the server");
            System.out.println("===========================================");
            
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Initialize database and create admin user if not exists
     */
    private static void initializeDatabase() {
        System.out.println("Initializing database...");
        try {
            // Test database connection
            Connection conn = DBConnection.getConnection();
            if (conn != null && !conn.isClosed()) {
                // Connection successful
                
                // Create all tables if they don't exist
                createAllTablesIfNotExist();
                
                // Add missing columns to existing tables
                addMissingColumns();
                
                // Check if admin user exists
                if (!adminUserExists()) {
                    createAdminUser();
                } else {
                    System.out.println("Admin user already exists.");
                }
                
                // Check if courses exist, if not insert sample courses
                if (!coursesExist()) {
                    insertSampleCourses();
                } else {
                    System.out.println("Courses already exist.");
                }
                
                }
        } catch (Exception e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create all database tables if they don't exist (preserve existing data)
     */
    private static void createAllTablesIfNotExist() {
        try (Statement stmt = DBConnection.getConnection().createStatement()) {
            System.out.println("Checking and creating missing tables...");
            
            // Create locations table if not exists
            if (!tableExists(stmt, "locations")) {
                stmt.executeUpdate("""
                    CREATE TABLE locations (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        address TEXT,
                        phone VARCHAR(50),
                        email VARCHAR(255),
                        manager_id INTEGER,
                        is_active BOOLEAN DEFAULT true,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
                System.out.println("Created locations table.");
                
                // Insert default location
                stmt.executeUpdate("INSERT INTO locations (name, address, phone, email) VALUES ('Main Campus', 'Nairobi, Kenya', '+254 700 000 000', 'info@ultimate.edu')");
            }
            
            // Create departments table if not exists
            if (!tableExists(stmt, "departments")) {
                stmt.executeUpdate("""
                    CREATE TABLE departments (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        description TEXT,
                        head_id INTEGER,
                        location_id INTEGER REFERENCES locations(id),
                        is_active BOOLEAN DEFAULT true,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
                System.out.println("Created departments table.");
            }
            
            // Create users table if not exists
            if (!tableExists(stmt, "users")) {
                stmt.executeUpdate("""
                    CREATE TABLE users (
                        id SERIAL PRIMARY KEY,
                        email VARCHAR(255) UNIQUE NOT NULL,
                        password_hash VARCHAR(255) NOT NULL,
                        first_name VARCHAR(100) NOT NULL,
                        last_name VARCHAR(100) NOT NULL,
                        full_name VARCHAR(255) NOT NULL,
                        phone VARCHAR(50),
                        alternative_phone VARCHAR(50),
                        date_of_birth DATE,
                        gender VARCHAR(20),
                        id_number VARCHAR(50),
                        profile_photo VARCHAR(500),
                        address TEXT,
                        city VARCHAR(100),
                        postal_code VARCHAR(20),
                        county VARCHAR(100),
                        role VARCHAR(20) NOT NULL DEFAULT 'applicant',
                        status VARCHAR(20) DEFAULT 'active',
                        department_id INTEGER REFERENCES departments(id),
                        location_id INTEGER REFERENCES locations(id),
                        position VARCHAR(100),
                        hire_date DATE,
                        salary DECIMAL(12, 2),
                        admission_number VARCHAR(50),
                        enrollment_date DATE,
                        graduation_date DATE,
                        guardian_name VARCHAR(255),
                        guardian_phone VARCHAR(50),
                        guardian_email VARCHAR(255),
                        emergency_contact_name VARCHAR(255),
                        emergency_contact_phone VARCHAR(50),
                        emergency_contact_relation VARCHAR(100),
                        medical_conditions TEXT,
                        allergies TEXT,
                        blood_group VARCHAR(10),
                        training_location VARCHAR(255),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT chk_role CHECK (role IN ('admin', 'staff', 'student', 'applicant', 'user')),
                        CONSTRAINT chk_status CHECK (status IN ('active', 'inactive', 'suspended'))
                    )
                """);
                System.out.println("Created users table.");
            }
            
            // Create JWT tokens table if not exists
            if (!tableExists(stmt, "jwt_tokens")) {
                stmt.executeUpdate("""
                    CREATE TABLE jwt_tokens (
                        id SERIAL PRIMARY KEY,
                        token VARCHAR(500) UNIQUE NOT NULL,
                        user_id INTEGER REFERENCES users(id),
                        expires_at TIMESTAMP NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
                System.out.println("Created jwt_tokens table.");
            }
            
            // Create courses table if not exists
            if (!tableExists(stmt, "courses")) {
                stmt.executeUpdate("""
                    CREATE TABLE courses (
                        id SERIAL PRIMARY KEY,
                        code VARCHAR(20) UNIQUE NOT NULL,
                        name VARCHAR(255) NOT NULL,
                        description TEXT,
                        category VARCHAR(50) NOT NULL,
                        duration_hours INTEGER,
                        duration_weeks INTEGER,
                        price DECIMAL(10, 2) NOT NULL,
                        requirements TEXT,
                        learning_outcomes TEXT,
                        department_id INTEGER REFERENCES departments(id),
                        course_level VARCHAR(50),
                        is_licensed BOOLEAN DEFAULT false,
                        license_type VARCHAR(50),
                        is_active BOOLEAN DEFAULT true,
                        is_featured BOOLEAN DEFAULT false,
                        created_by INTEGER REFERENCES users(id),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
                System.out.println("Created courses table.");
            }
            
            // Create classes table if not exists
            if (!tableExists(stmt, "classes")) {
                stmt.executeUpdate("""
                    CREATE TABLE classes (
                        id SERIAL PRIMARY KEY,
                        course_id INTEGER REFERENCES courses(id) ON DELETE RESTRICT,
                        name VARCHAR(100) NOT NULL,
                        code VARCHAR(20) UNIQUE NOT NULL,
                        description TEXT,
                        location_id INTEGER REFERENCES locations(id),
                        start_date DATE,
                        end_date DATE,
                        start_time TIME,
                        end_time TIME,
                        days_of_week VARCHAR(50),
                        instructor_id INTEGER REFERENCES users(id),
                        max_students INTEGER DEFAULT 30,
                        current_students INTEGER DEFAULT 0,
                        status VARCHAR(20) DEFAULT 'open',
                        class_fee DECIMAL(10, 2),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
                System.out.println("Created classes table.");
            }
            
            // Create enrollments table if not exists
            if (!tableExists(stmt, "enrollments")) {
                stmt.executeUpdate("""
                    CREATE TABLE enrollments (
                        id SERIAL PRIMARY KEY,
                        student_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
                        class_id INTEGER REFERENCES classes(id) ON DELETE CASCADE,
                        enrollment_number VARCHAR(50) UNIQUE NOT NULL,
                        enrollment_date DATE NOT NULL,
                        status VARCHAR(20) DEFAULT 'enrolled',
                        progress_percentage DECIMAL(5, 2) DEFAULT 0.00,
                        start_time TIMESTAMP,
                        end_time TIMESTAMP,
                        fee_amount DECIMAL(10, 2) NOT NULL,
                        fee_paid DECIMAL(10, 2) DEFAULT 0.00,
                        fee_balance DECIMAL(10, 2) DEFAULT 0.00,
                        payment_status VARCHAR(20) DEFAULT 'unpaid',
                        completion_date DATE,
                        certificate_number VARCHAR(50),
                        notes TEXT,
                        license_type VARCHAR(50) DEFAULT 'Class B',
                        course_id INTEGER REFERENCES courses(id),
                        driving_course VARCHAR(100),
                        computer_course VARCHAR(100),
                        location_id INTEGER, -- Can be NULL if location doesn't exist in locations table
                        transmission VARCHAR(50),
                        preferred_schedule VARCHAR(20),
                        preferred_start DATE,
                        emergency_contact_name VARCHAR(255),
                        emergency_contact_phone VARCHAR(50),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT chk_enrollment_status CHECK (status IN ('enrolled', 'active', 'completed', 'dropped', 'suspended')),
                        CONSTRAINT chk_payment_status CHECK (payment_status IN ('unpaid', 'partial', 'paid')),
                        CONSTRAINT uq_student_class UNIQUE (student_id, class_id)
                    )
                """);
                System.out.println("Created enrollments table.");
            }
            
            // Enrollments table schema is now defined in database.sql
            
            // Create applications table if not exists
            if (!tableExists(stmt, "applications")) {
                stmt.executeUpdate("""
                    CREATE TABLE applications (
                        id SERIAL PRIMARY KEY,
                        user_id INTEGER REFERENCES users(id),
                        first_name VARCHAR(100) NOT NULL,
                        last_name VARCHAR(100) NOT NULL,
                        email VARCHAR(255) NOT NULL,
                        phone VARCHAR(50) NOT NULL,
                        date_of_birth DATE,
                        address TEXT,
                        city VARCHAR(100),
                        postal_code VARCHAR(20),
                        id_number VARCHAR(50),
                        license_type VARCHAR(50) DEFAULT 'Class B',
                        course_id INTEGER REFERENCES courses(id),
                        driving_course VARCHAR(100),
                        computer_course VARCHAR(100),
                        location_id INTEGER, -- Can be NULL if location doesn't exist in locations table
                        transmission VARCHAR(50),
                        preferred_schedule VARCHAR(20),
                        preferred_start DATE,
                        emergency_contact_name VARCHAR(255),
                        emergency_contact_phone VARCHAR(50),
                        comments TEXT,
                        medical_conditions TEXT,
                        previous_driving_experience BOOLEAN DEFAULT FALSE,
                        status VARCHAR(50) DEFAULT 'pending',
                        training_location VARCHAR(255),
                        school_fees DECIMAL(10, 2) DEFAULT 0.00,
                        fees_paid DECIMAL(10, 2) DEFAULT 0.00,
                        fees_balance DECIMAL(10, 2) DEFAULT 0.00,
                        payment_status VARCHAR(50) DEFAULT 'unpaid',
                        payment_method VARCHAR(50),
                        staff_id INTEGER REFERENCES users(id),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
                System.out.println("Created applications table.");
            }
            
            // Create attendance table if not exists
            if (!tableExists(stmt, "attendance")) {
                stmt.executeUpdate("""
                    CREATE TABLE attendance (
                        id SERIAL PRIMARY KEY,
                        student_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
                        class_id INTEGER REFERENCES classes(id) ON DELETE CASCADE,
                        enrollment_id INTEGER REFERENCES enrollments(id) ON DELETE CASCADE,
                        date DATE NOT NULL,
                        time_in TIME,
                        time_out TIME,
                        status VARCHAR(20) NOT NULL,
                        notes TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
                System.out.println("Created attendance table.");
            }
            
            // Attendance table schema is now defined in database.sql
            
            // Create contact_messages table if not exists
            if (!tableExists(stmt, "contact_messages")) {
                stmt.executeUpdate("""
                    CREATE TABLE contact_messages (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        email VARCHAR(255) NOT NULL,
                        phone VARCHAR(50),
                        subject VARCHAR(255),
                        message TEXT NOT NULL,
                        status VARCHAR(50) DEFAULT 'new',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
                System.out.println("Created contact_messages table.");
            }
            
            // Create mpesa_messages table if not exists
            if (!tableExists(stmt, "mpesa_messages")) {
                stmt.executeUpdate("""
                    CREATE TABLE mpesa_messages (
                        id SERIAL PRIMARY KEY,
                        application_id INTEGER REFERENCES applications(id),
                        message TEXT NOT NULL,
                        phone VARCHAR(50),
                        amount DECIMAL(10, 2) DEFAULT 0.00,
                        mpesa_code VARCHAR(50),
                        status VARCHAR(50) DEFAULT 'pending',
                        verified_by INTEGER REFERENCES users(id),
                        verified_at TIMESTAMP,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
                System.out.println("Created mpesa_messages table.");
            }
            
            // Create indexes
            createIndexesIfNotExist(stmt);
            
            System.out.println("All tables verified/created successfully.");
        } catch (Exception e) {
            System.err.println("Failed to create tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check if a table exists
     */
    private static boolean tableExists(Statement stmt, String tableName) {
        try {
            ResultSet rs = stmt.executeQuery("SELECT to_regclass('public." + tableName + "')");
            boolean exists = rs.next() && rs.getString(1) != null;
            rs.close();
            return exists;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Create indexes if they don't exist
     */
    private static void createIndexesIfNotExist(Statement stmt) {
        try {
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_users_role ON users(role)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_users_location ON users(location_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_applications_user_id ON applications(user_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_applications_status ON applications(status)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_applications_training_location ON applications(training_location)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_applications_payment_status ON applications(payment_status)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_applications_staff_id ON applications(staff_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_contact_messages_status ON contact_messages(status)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_jwt_tokens_expires_at ON jwt_tokens(expires_at)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_mpesa_messages_application_id ON mpesa_messages(application_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_mpesa_messages_status ON mpesa_messages(status)");
        } catch (Exception e) {
            // Ignore index creation errors (they may already exist)
        }
    }
    
    /**
     * Add missing columns to existing tables
     */
    private static void addMissingColumns() {
        System.out.println("Checking for missing columns...");
        try (Statement stmt = DBConnection.getConnection().createStatement()) {
            // Add missing columns to enrollments table
            addColumnIfNotExists(stmt, "enrollments", "location_id", "INTEGER");
            addColumnIfNotExists(stmt, "enrollments", "license_type", "VARCHAR(50)");
            addColumnIfNotExists(stmt, "enrollments", "driving_course", "VARCHAR(100)");
            addColumnIfNotExists(stmt, "enrollments", "computer_course", "VARCHAR(100)");
            addColumnIfNotExists(stmt, "enrollments", "transmission", "VARCHAR(50)");
            addColumnIfNotExists(stmt, "enrollments", "preferred_schedule", "VARCHAR(50)");
            addColumnIfNotExists(stmt, "enrollments", "training_location", "VARCHAR(200)");
            addColumnIfNotExists(stmt, "enrollments", "preferred_start", "DATE");
            addColumnIfNotExists(stmt, "enrollments", "emergency_contact_name", "VARCHAR(255)");
            addColumnIfNotExists(stmt, "enrollments", "emergency_contact_phone", "VARCHAR(50)");
            
            // Add missing columns to applications table
            addColumnIfNotExists(stmt, "applications", "driving_experience", "VARCHAR(20)");
            addColumnIfNotExists(stmt, "applications", "license_type", "VARCHAR(50) DEFAULT 'Class B'");
            
            // Migrate existing enrollments that don't have course data
            migrateExistingEnrollments(stmt);
            
            System.out.println("Missing columns check completed.");
        } catch (Exception e) {
            System.err.println("Error adding missing columns: " + e.getMessage());
        }
    }
    
    /**
     * Migrate existing enrollments to populate course fields based on fee amounts
     */
    private static void migrateExistingEnrollments(Statement stmt) {
        try {
            // First, let's see what enrollments we have and their fee amounts
            ResultSet rs = stmt.executeQuery("SELECT id, fee_amount, license_type, driving_course, computer_course FROM enrollments");
            System.out.println("Found enrollments, checking for missing course data...");
            
            while (rs.next()) {
                int id = rs.getInt("id");
                double feeAmount = rs.getDouble("fee_amount");
                String licenseType = rs.getString("license_type");
                String drivingCourse = rs.getString("driving_course");
                String computerCourse = rs.getString("computer_course");
                
                boolean needsUpdate = false;
                String updateSql = "UPDATE enrollments SET ";
                
                // If license_type is not set and fee >= 15000, set to Class B
                if ((licenseType == null || licenseType.isEmpty()) && feeAmount >= 15000) {
                    updateSql += "license_type = 'Class B'";
                    needsUpdate = true;
                }
                
                // If driving_course is not set and fee >= 10000, set to Defensive Driving
                if ((drivingCourse == null || drivingCourse.isEmpty()) && feeAmount >= 10000 && feeAmount < 20000) {
                    if (needsUpdate) updateSql += ", ";
                    updateSql += "driving_course = 'Defensive Driving'";
                    needsUpdate = true;
                }
                
                // If computer_course is not set and fee < 15000 (likely computer course)
                if ((computerCourse == null || computerCourse.isEmpty()) && feeAmount <= 12000) {
                    if (needsUpdate) updateSql += ", ";
                    if (feeAmount == 5000) {
                        updateSql += "computer_course = 'Computer Basics'";
                    } else if (feeAmount == 10000) {
                        updateSql += "computer_course = 'Computer Accounting'";
                    } else if (feeAmount == 8000) {
                        updateSql += "computer_course = 'Secretarial'";
                    } else if (feeAmount == 7500) {
                        updateSql += "computer_course = 'SPSS'";
                    } else if (feeAmount == 12000) {
                        updateSql += "computer_course = 'Graphic Design'";
                    } else if (feeAmount == 25000) {
                        updateSql += "computer_course = 'Programming'";
                    } else if (feeAmount <= 10000) {
                        updateSql += "computer_course = 'Computer Basics'";
                    }
                    needsUpdate = true;
                }
                
                if (needsUpdate) {
                    updateSql += " WHERE id = " + id;
                    stmt.executeUpdate(updateSql);
                    System.out.println("Updated enrollment #" + id + " with fee amount " + feeAmount);
                }
            }
            rs.close();
            
            System.out.println("Enrollment migration completed.");
        } catch (Exception e) {
            System.err.println("Error migrating enrollments: " + e.getMessage());
        }
    }
    
    /**
     * Add a column to a table if it doesn't exist
     */
    private static void addColumnIfNotExists(Statement stmt, String tableName, String columnName, String columnDefinition) {
        try {
            // Check if column exists
            ResultSet rs = stmt.executeQuery(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = '" + tableName + "' AND column_name = '" + columnName + "'");
            if (!rs.next()) {
                // Column doesn't exist, add it
                stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
                System.out.println("Added missing column: " + tableName + "." + columnName);
            }
            rs.close();
        } catch (Exception e) {
            // Column may already exist or other error, ignore
        }
    }
    
    /**
     * Check if admin user exists
     */
    private static boolean adminUserExists() {
        String sql = "SELECT id FROM users WHERE email = '" + ADMIN_EMAIL + "'";
        try (Statement stmt = DBConnection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Create admin user
     */
    private static void createAdminUser() {
        String passwordHash = DBConnection.hashPassword(ADMIN_PASSWORD);
        String sql = "INSERT INTO users (email, password_hash, first_name, last_name, full_name, phone, role) " +
                     "VALUES ('" + ADMIN_EMAIL + "', '" + passwordHash + "', 'System', 'Administrator', 'Administrator', '+254 700 000 000', 'admin')";
        try (Statement stmt = DBConnection.getConnection().createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("Admin user created successfully!");
            System.out.println("  Email: " + ADMIN_EMAIL);
            System.out.println("  Password: " + ADMIN_PASSWORD);
        } catch (Exception e) {
            System.err.println("Failed to create admin user: " + e.getMessage());
        }
        
        // Also create the user timo.munyiri@gmail.com as admin
        createUserIfNotExists("timo.munyiri@gmail.com", "admin123", "Timo", "Munyiri", "+254 700 000 000", "admin");
    }
    
    /**
     * Create a user if not exists
     */
    private static void createUserIfNotExists(String email, String password, String firstName, String lastName, String phone, String role) {
        try {
            // Check if user exists
            String checkSql = "SELECT id FROM users WHERE email = '" + email + "'";
            try (Statement stmt = DBConnection.getConnection().createStatement()) {
                ResultSet rs = stmt.executeQuery(checkSql);
                if (rs.next()) {
                    System.out.println("User already exists: " + email);
                    // Update role to admin
                    String updateSql = "UPDATE users SET role = '" + role + "' WHERE email = '" + email + "'";
                    stmt.executeUpdate(updateSql);
                    System.out.println("User role updated to: " + role);
                    return;
                }
            }
            
            // Create user
            String passwordHash = DBConnection.hashPassword(password);
            String fullName = firstName + " " + lastName;
            String insertSql = "INSERT INTO users (email, password_hash, first_name, last_name, full_name, phone, role) " +
                               "VALUES ('" + email + "', '" + passwordHash + "', '" + firstName + "', '" + lastName + "', '" + fullName + "', '" + phone + "', '" + role + "')";
            try (Statement stmt = DBConnection.getConnection().createStatement()) {
                stmt.executeUpdate(insertSql);
                System.out.println("User created successfully!");
                System.out.println("  Email: " + email);
                System.out.println("  Password: " + password);
                System.out.println("  Role: " + role);
            }
        } catch (Exception e) {
            System.err.println("Failed to create user " + email + ": " + e.getMessage());
        }
    }
    
    /**
     * Check if courses exist
     */
    private static boolean coursesExist() {
        String sql = "SELECT id FROM courses LIMIT 1";
        try (Statement stmt = DBConnection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Insert sample courses
     */
    private static void insertSampleCourses() {
        // Insert courses with the new schema (code, name, description, category, duration_hours, price, requirements)
        String[] courses = {
            "('CDL-B', 'Class B License', 'Light vehicle driving course for cars and small trucks.', 'Driving', 30, 15000.00, 'Valid ID, Minimum age 18 years')",
            "('CDL-C', 'Class C License', 'Heavy commercial vehicle driving course.', 'Driving', 45, 25000.00, 'Valid Class B license, Minimum age 21 years')",
            "('CDL-D', 'Class D License', 'Professional driving course for passenger vehicles.', 'Driving', 50, 30000.00, 'Valid ID, Minimum age 24 years')",
            "('MOTO-01', 'Motorcycle Training', 'Motorcycle riding course.', 'Driving', 15, 8000.00, 'Valid ID, Minimum age 16 years')",
            "('DEF-01', 'Defensive Driving', 'Advanced driving techniques.', 'Driving', 20, 10000.00, 'Valid driving license, Minimum age 18 years')",
            "('REF-01', 'Refresher Course', 'Short course for experienced drivers.', 'Driving', 10, 5000.00, 'Valid driving license')",
            "('COMP-01', 'Computer Basics', 'Introduction to computers.', 'Computer', 20, 5000.00, 'No prerequisites')",
            "('COMP-02', 'Computer Accounting', 'Accounting software training.', 'Computer', 30, 10000.00, 'Basic computer knowledge')",
            "('COMP-03', 'Computer Secretarial', 'Office administration training.', 'Computer', 25, 8000.00, 'Basic computer knowledge')",
            "('COMP-04', 'Graphic Design', 'Master graphic design.', 'Computer', 35, 12000.00, 'Basic computer knowledge')",
            "('COMP-05', 'Computer Programming', 'Comprehensive programming course.', 'Computer', 60, 25000.00, 'Basic computer knowledge')",
            "('COMP-06', 'SPSS', 'Statistical analysis training.', 'Computer', 20, 7500.00, 'Basic computer knowledge')",
            // Additional courses (shown in 'Additional Courses' section with book icon)
            "('ADD-01', 'Advanced Driving Techniques', 'Advanced course for experienced drivers.', 'Driving', 30, 20000.00, 'Class B License required')",
            "('ADD-02', 'Fleet Management', 'Learn to manage vehicle fleets.', 'Driving', 40, 35000.00, 'Class C License required')",
            "('ADD-03', 'Road Safety Awareness', 'Essential road safety knowledge.', 'Defensive', 15, 8000.00, 'None')"
        };
        
        String sql = "INSERT INTO courses (code, name, description, category, duration_hours, price, requirements) VALUES " + 
                     String.join(", ", courses);
        try (Statement stmt = DBConnection.getConnection().createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("Sample courses inserted successfully!");
        } catch (Exception e) {
            System.err.println("Failed to insert sample courses: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get local IP address
     */
    private static String getLocalIpAddress() {
        try {
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }
}
