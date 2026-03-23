-- ============================================================================
-- ULTIMATE DEFENSIVE DRIVING SCHOOL - DATABASE EXPORT
-- ============================================================================
-- Extracted from UltimateServer.java
-- 
-- Default login credentials:
-- - admin@ultimate.edu / admin123
-- - timo.munyiri@gmail.com / admin123
--
-- Import command:
-- psql -U your_username -d ultimate_driving_school -f ultimate_db.sql
--
-- ============================================================================

-- Create database
DROP DATABASE IF EXISTS ultimate_driving_school;
CREATE DATABASE ultimate_driving_school;
\c ultimate_driving_school;

-- ============================================================================
-- LOCATIONS TABLE
-- ============================================================================

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
);

-- ============================================================================
-- DEPARTMENTS TABLE
-- ============================================================================

CREATE TABLE departments (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    head_id INTEGER,
    location_id INTEGER REFERENCES locations(id),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- USERS TABLE
-- ============================================================================

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
);

-- ============================================================================
-- JWT TOKENS TABLE
-- ============================================================================

CREATE TABLE jwt_tokens (
    id SERIAL PRIMARY KEY,
    token VARCHAR(500) UNIQUE NOT NULL,
    user_id INTEGER REFERENCES users(id),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- COURSES TABLE
-- ============================================================================

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
);

-- ============================================================================
-- CLASSES TABLE
-- ============================================================================

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
);

-- ============================================================================
-- ENROLLMENTS TABLE
-- ============================================================================

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
    location_id INTEGER,
    transmission VARCHAR(50),
    preferred_schedule VARCHAR(20),
    preferred_start DATE,
    emergency_contact_name VARCHAR(255),
    emergency_contact_phone VARCHAR(50),
    training_location VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_enrollment_status CHECK (status IN ('enrolled', 'active', 'completed', 'dropped', 'suspended')),
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('unpaid', 'partial', 'paid')),
    CONSTRAINT uq_student_class UNIQUE (student_id, class_id)
);

-- ============================================================================
-- APPLICATIONS TABLE
-- ============================================================================

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
    location_id INTEGER,
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
);

-- ============================================================================
-- ATTENDANCE TABLE
-- ============================================================================

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
    location VARCHAR(255),
    license_type VARCHAR(50),
    driving_course VARCHAR(100),
    computer_course VARCHAR(100),
    transmission VARCHAR(50),
    preferred_schedule VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_attendance_status CHECK (status IN ('present', 'absent', 'late', 'excused')),
    CONSTRAINT uq_student_date_class UNIQUE (student_id, class_id, date)
);

-- ============================================================================
-- CONTACT_MESSAGES TABLE
-- ============================================================================

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
);

-- ============================================================================
-- MPESA_MESSAGES TABLE
-- ============================================================================

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
);

-- ============================================================================
-- DEFAULT DATA
-- ============================================================================

-- Default location
-- ============================================================================
-- ULTIMATE DEFENSIVE DRIVING SCHOOL - DATABASE DATA EXPORT (CLEAN)
-- ============================================================================
-- Data extracted from local PostgreSQL database
-- Only includes columns that are actually saved by the Java application
-- ============================================================================

-- ============================================================================
-- LOCATIONS (only columns that are saved)
-- ============================================================================
INSERT INTO locations (id, name, address, phone, email) VALUES 
(3, 'Githunguri Branch', 'Githunguri Town, Opposite Equity Bank, Nairobi', '+254 790722419', 'githunguri@ultimatedefensive.co.ke'),
(4, 'Mwiki Center', 'Mwiki Shopping Center, Nairobi', '+254 711111111', 'mwiki@ultimatedefensive.co.ke'),
(5, 'Githurai 44A', 'Next to Equity Bank, Unaitas Building, Nairobi', '+254 722222222', 'githurai44a@ultimatedefensive.co.ke'),
(6, 'Githurai 44B', 'Opposite Kiambu Stage, Nairobi', '+254 733333333', 'githurai44b@ultimatedefensive.co.ke'),
(7, 'Zimmerman Branch', 'Next to Main Stage (Base), Nairobi', '+254 744444444', 'zimmerman@ultimatedefensive.co.ke'),
(9, 'Ruiru Branch', 'Ruiru Town Center, Near Roundabout', '+254 766666666', 'ruiru@ultimatedefensive.co.ke'),
(10, 'Thika Road Center', 'Taj Mall Building, Nairobi', '+254 777777777', 'thikaroad@ultimatedefensive.co.ke'),
(11, 'Westlands Branch', 'Sarit Centre Area, Nairobi', '+254 788888888', 'westlands@ultimatedefensive.co.ke'),
(2, 'Sunton', 'Sunton Shopping Center, Nairobi', '+254 700 000000', 'info@ultimatedefensive.co.ke'),
(15, 'Main Campus - Nairobi', 'Thika road Mall', '+254 790722419', 'main@ultimate.edu'),
(1, 'Main Campus', NULL, NULL, NULL);

-- ============================================================================
-- DEPARTMENTS
-- ============================================================================
INSERT INTO departments (id, name, description, head_id, location_id) VALUES 
(1, 'DRIVING', 'Driving Department HOD', 1, 2);

-- ============================================================================
-- USERS (only columns that are saved)
-- ============================================================================
INSERT INTO users (id, email, password_hash, first_name, last_name, full_name, phone, role, status, department_id, location_id, position, training_location) VALUES 
(2, 'timo.munyiri@gmail.com', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Timo', 'Munyiri', 'Timo Munyiri', '+254 700 000 000', 'admin', 'active', NULL, 2, NULL, 'Sunton'),
(8, 'admin@ultimate.edu', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'System', 'Administrator', 'Administrator', '+254 700 000 000', 'staff', 'active', NULL, 2, NULL, 'Sunton - Sunton Shopping Center, Nairobi'),
(6, 'motivkama@gmail.com', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Motiv', 'Kamau', 'Motiv Kamau', '+254790722419', 'instructor', 'active', NULL, 2, NULL, 'Sunton - Sunton Shopping Center, Nairobi'),
(7, '', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Timothy', 'Munyiri', 'Timothy Munyiri', '+254 700 000 000', 'staff', 'active', NULL, 2, NULL, 'Sunton'),
(3, 'motiv.mot@ultimat.udd', '39fbaa5f806c772ca53ddfc3bf4419faa9894336c08787bd3ec28fd1e97ec8bb', 'Timothy', 'Munyiri', 'Timothy ', '0790722419', 'user', 'active', NULL, 1, NULL, '-- Select Location --'),
(4, 'john@gmial.com', '96d9632f363564cc3032521409cf22a852f2032eec099ed5967c0d000cec607a', 'John', 'Maina', 'John Main', '0790722419', 'applicant', 'active', NULL, 2, NULL, NULL),
(5, 'mark@gmail.com', '6201eb4dccc956cc4fa3a78dca0c2888177ec52efd48f125df214f046eb43138', 'Mark', 'Waweru', 'Mark Waweru', '0790000000', 'applicant', 'active', NULL, 7, NULL, NULL);

-- ============================================================================
-- COURSES (only columns that are saved)
-- ============================================================================
INSERT INTO courses (id, code, name, description, category, duration_hours, duration_weeks, price, requirements, is_active) VALUES 
(1, 'CDL-B', 'Class B License', 'Light vehicle driving course for cars and small trucks.', 'Driving', 30, NULL, 15000.00, 'Valid ID, Minimum age 18 years', true),
(2, 'CDL-C', 'Class C License', 'Heavy commercial vehicle driving course.', 'Driving', 45, NULL, 25000.00, 'Valid Class B license, Minimum age 21 years', true),
(3, 'CDL-D', 'Class D License', 'Professional driving course for passenger vehicles.', 'driving', 50, 0, 30000.00, 'Valid ID, Minimum age 24 years', true),
(4, 'MOTO-01', 'Motorcycle Training', 'Motorcycle riding course.', 'Driving', 15, NULL, 8000.00, 'Valid ID, Minimum age 16 years', true),
(5, 'DEF-01', 'Defensive Driving', 'Advanced driving techniques.', 'Driving', 20, NULL, 10000.00, 'Valid driving license, Minimum age 18 years', true),
(6, 'REF-01', 'Refresher Course', 'Short course for experienced drivers.', 'Driving', 10, NULL, 5000.00, 'Valid driving license', true),
(7, 'COMP-01', 'Computer Basics', 'Introduction to computers.', 'Computer', 20, NULL, 5000.00, 'No prerequisites', false),
(8, 'COMP-02', 'Computer Accounting', 'Accounting software training.', 'Computer', 30, NULL, 10000.00, 'Basic computer knowledge', true),
(9, 'COMP-03', 'Computer Secretarial', 'Office administration training.', 'Computer', 25, NULL, 8000.00, 'Basic computer knowledge', true),
(10, 'COMP-04', 'Graphic Design', 'Master graphic design.', 'Computer', 35, NULL, 12000.00, 'Basic computer knowledge', true),
(11, 'COMP-05', 'Computer Programming', 'Comprehensive programming course.', 'Computer', 60, NULL, 25000.00, 'Basic computer knowledge', true),
(12, 'COMP-06', 'SPSS', 'Statistical analysis training.', 'Computer', 20, NULL, 7500.00, 'Basic computer knowledge', true),
(13, 'ADD-01', 'Advanced Driving Techniques', 'Advanced course for experienced drivers.', 'Driving', 30, NULL, 20000.00, 'Class B License required', true),
(14, 'ADD-02', 'Fleet Management', 'Learn to manage vehicle fleets.', 'Driving', 40, NULL, 35000.00, 'Class C License required', true),
(15, 'ADD-03', 'Road Safety Awareness', 'Essential road safety knowledge.', 'Defensive', 15, NULL, 8000.00, 'None', true);


-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_location ON users(location_id);
CREATE INDEX idx_applications_user_id ON applications(user_id);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_applications_training_location ON applications(training_location);
CREATE INDEX idx_applications_payment_status ON applications(payment_status);
CREATE INDEX idx_applications_staff_id ON applications(staff_id);
