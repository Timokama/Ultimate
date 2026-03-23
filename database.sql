-- ============================================================================
-- ULTIMATE DEFENSIVE DRIVING SCHOOL - DATABASE SCHEMA
-- ============================================================================
--
-- A comprehensive school management system with:
-- - Multi-role authentication (Admin, Staff, Student)
-- - Student enrollment and management
-- - Course and class scheduling
-- - Attendance tracking
-- - Grades and results management
-- - Fee management with M-Pesa integration
-- - Staff management
-- - Comprehensive reporting
--
-- Author: Ultimate Defensive Driving School Development Team
-- Date: 2026
--
-- USAGE:
--   psql -U postgres -f database.sql
--
-- ============================================================================

-- ============================================================================
-- SECTION 1: CREATE DATABASE
-- ============================================================================

DROP DATABASE IF EXISTS ultimate_driving_school;
CREATE DATABASE ultimate_driving_school;
\c ultimate_driving_school;

-- ============================================================================
-- SECTION 2: LOCATIONS/BRANCHES TABLE
-- ============================================================================

CREATE TABLE locations (
    id                  SERIAL PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    address             TEXT,
    phone               VARCHAR(50),
    email               VARCHAR(255),
    manager_id          INTEGER,
    is_active           BOOLEAN DEFAULT true,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- SECTION 3: DEPARTMENTS TABLE
-- ============================================================================

CREATE TABLE departments (
    id                  SERIAL PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    description         TEXT,
    head_id             INTEGER,
    location_id         INTEGER REFERENCES locations(id),
    is_active           BOOLEAN DEFAULT true,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- SECTION 4: USERS TABLE
-- ============================================================================

CREATE TABLE users (
    id                  SERIAL PRIMARY KEY,
    email               VARCHAR(255) UNIQUE NOT NULL,
    password_hash       VARCHAR(64) NOT NULL,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    full_name           VARCHAR(255) NOT NULL,
    phone               VARCHAR(50),
    alternative_phone   VARCHAR(50),
    date_of_birth       DATE,
    gender              VARCHAR(20),
    id_number           VARCHAR(50),
    profile_photo       VARCHAR(500),
    address             TEXT,
    city                VARCHAR(100),
    postal_code         VARCHAR(20),
    county              VARCHAR(100),
    role                VARCHAR(20) NOT NULL,
    status              VARCHAR(20) DEFAULT 'active',
    department_id       INTEGER REFERENCES departments(id),
    location_id         INTEGER REFERENCES locations(id),
    position            VARCHAR(100),
    hire_date           DATE,
    salary              DECIMAL(12, 2),
    admission_number    VARCHAR(50),
    enrollment_date     DATE,
    graduation_date     DATE,
    guardian_name       VARCHAR(255),
    guardian_phone      VARCHAR(50),
    guardian_email      VARCHAR(255),
    emergency_contact_name     VARCHAR(255),
    emergency_contact_phone    VARCHAR(50),
    emergency_contact_relation VARCHAR(100),
    medical_conditions  TEXT,
    allergies           TEXT,
    blood_group         VARCHAR(10),
    training_location   VARCHAR(255),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_role CHECK (role IN ('admin', 'staff', 'student', 'applicant', 'user')),
    CONSTRAINT chk_status CHECK (status IN ('active', 'inactive', 'suspended'))
);

-- ============================================================================
-- SECTION 5: COURSES TABLE
-- ============================================================================

CREATE TABLE courses (
    id                  SERIAL PRIMARY KEY,
    code                VARCHAR(20) UNIQUE NOT NULL,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    category            VARCHAR(50) NOT NULL,
    duration_hours      INTEGER,
    duration_weeks       INTEGER,
    price               DECIMAL(10, 2) NOT NULL,
    requirements         TEXT,
    learning_outcomes    TEXT,
    department_id       INTEGER REFERENCES departments(id),
    course_level        VARCHAR(50),
    is_licensed         BOOLEAN DEFAULT false,
    license_type        VARCHAR(50),
    is_active           BOOLEAN DEFAULT true,
    is_featured         BOOLEAN DEFAULT false,
    created_by          INTEGER REFERENCES users(id),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- SECTION 6: CLASSES TABLE
-- ============================================================================

CREATE TABLE classes (
    id                  SERIAL PRIMARY KEY,
    course_id           INTEGER REFERENCES courses(id) ON DELETE RESTRICT,
    name                VARCHAR(100) NOT NULL,
    code                VARCHAR(20) UNIQUE NOT NULL,
    description         TEXT,
    location_id         INTEGER REFERENCES locations(id),
    start_date          DATE,
    end_date            DATE,
    start_time          TIME,
    end_time            TIME,
    days_of_week        VARCHAR(50),
    instructor_id       INTEGER REFERENCES users(id),
    max_students        INTEGER DEFAULT 30,
    current_students    INTEGER DEFAULT 0,
    status              VARCHAR(20) DEFAULT 'open',
    class_fee           DECIMAL(10, 2),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- SECTION 7: ENROLLMENTS TABLE
-- ============================================================================

CREATE TABLE enrollments (
    id                  SERIAL PRIMARY KEY,
    student_id          INTEGER REFERENCES users(id) ON DELETE CASCADE,
    class_id            INTEGER REFERENCES classes(id) ON DELETE CASCADE,
    enrollment_number   VARCHAR(50) UNIQUE NOT NULL,
    enrollment_date     DATE NOT NULL,
    status              VARCHAR(20) DEFAULT 'enrolled',
    progress_percentage DECIMAL(5, 2) DEFAULT 0.00,
    start_time          TIMESTAMP,
    end_time            TIMESTAMP,
    fee_amount          DECIMAL(10, 2) NOT NULL,
    fee_paid            DECIMAL(10, 2) DEFAULT 0.00,
    fee_balance         DECIMAL(10, 2) DEFAULT 0.00,
    payment_status      VARCHAR(20) DEFAULT 'unpaid',
    completion_date     DATE,
    certificate_number  VARCHAR(50),
    notes               TEXT,
    location_id         INTEGER REFERENCES locations(id),
    license_type        VARCHAR(50),
    driving_course      VARCHAR(100),
    computer_course     VARCHAR(100),
    transmission        VARCHAR(20),
    preferred_schedule  VARCHAR(50),
    training_location  VARCHAR(200),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_enrollment_status CHECK (status IN ('enrolled', 'active', 'completed', 'dropped', 'suspended')),
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('unpaid', 'partial', 'paid')),
    CONSTRAINT uq_student_class UNIQUE (student_id, class_id)
);

-- ============================================================================
-- SECTION 8: ATTENDANCE TABLE
-- ============================================================================

CREATE TABLE attendance (
    id                  SERIAL PRIMARY KEY,
    student_id          INTEGER REFERENCES users(id) ON DELETE CASCADE,
    class_id            INTEGER REFERENCES classes(id) ON DELETE CASCADE,
    enrollment_id       INTEGER REFERENCES enrollments(id) ON DELETE CASCADE,
    date                DATE NOT NULL,
    time_in             TIME,
    time_out            TIME,
    status              VARCHAR(20) NOT NULL,
    expected_hours      DECIMAL(5, 2),
    actual_hours        DECIMAL(5, 2),
    notes               TEXT,
    marked_by           INTEGER REFERENCES users(id),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_attendance_status CHECK (status IN ('present', 'absent', 'late', 'excused')),
    CONSTRAINT uq_student_date_class UNIQUE (student_id, class_id, date)
);

-- ============================================================================
-- SECTION 9: ASSESSMENTS TABLE
-- ============================================================================

CREATE TABLE assessments (
    id                  SERIAL PRIMARY KEY,
    class_id            INTEGER REFERENCES classes(id) ON DELETE CASCADE,
    name                VARCHAR(100) NOT NULL,
    assessment_type     VARCHAR(50) NOT NULL,
    description         TEXT,
    max_score           DECIMAL(5, 2) NOT NULL,
    passing_score       DECIMAL(5, 2) DEFAULT 0.00,
    weight              DECIMAL(5, 2) DEFAULT 1.00,
    assessment_date     DATE,
    due_date            DATE,
    is_published        BOOLEAN DEFAULT false,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_assessment_type CHECK (assessment_type IN ('quiz', 'test', 'exam', 'practical', 'assignment'))
);

-- ============================================================================
-- SECTION 10: GRADES TABLE
-- ============================================================================

CREATE TABLE grades (
    id                  SERIAL PRIMARY KEY,
    student_id          INTEGER REFERENCES users(id) ON DELETE CASCADE,
    assessment_id       INTEGER REFERENCES assessments(id) ON DELETE CASCADE,
    enrollment_id       INTEGER REFERENCES enrollments(id) ON DELETE CASCADE,
    score               DECIMAL(5, 2) NOT NULL,
    grade               VARCHAR(5),
    percentage          DECIMAL(5, 2),
    remarks             TEXT,
    is_released        BOOLEAN DEFAULT false,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_assessment UNIQUE (student_id, assessment_id)
);

-- ============================================================================
-- SECTION 11: FEES TABLE
-- ============================================================================

CREATE TABLE fees (
    id                  SERIAL PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    description         TEXT,
    amount              DECIMAL(10, 2) NOT NULL,
    fee_type            VARCHAR(50) NOT NULL,
    course_id           INTEGER REFERENCES courses(id),
    is_mandatory        BOOLEAN DEFAULT true,
    due_date            DATE,
    is_active           BOOLEAN DEFAULT true,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- SECTION 12: FEE PAYMENTS TABLE
-- ============================================================================

CREATE TABLE fee_payments (
    id                  SERIAL PRIMARY KEY,
    student_id          INTEGER REFERENCES users(id) ON DELETE CASCADE,
    enrollment_id       INTEGER REFERENCES enrollments(id) ON DELETE CASCADE,
    fee_id              INTEGER REFERENCES fees(id),
    amount              DECIMAL(10, 2) NOT NULL,
    payment_method      VARCHAR(50) NOT NULL,
    payment_date        DATE NOT NULL,
    reference_number    VARCHAR(100),
    receipt_number      VARCHAR(50) UNIQUE,
    status              VARCHAR(20) DEFAULT 'pending',
    notes               TEXT,
    processed_by        INTEGER REFERENCES users(id),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- SECTION 13: M-PESA TRANSACTIONS TABLE
-- ============================================================================

CREATE TABLE mpesa_transactions (
    id                  SERIAL PRIMARY KEY,
    transaction_id       VARCHAR(50) UNIQUE NOT NULL,
    student_id          INTEGER REFERENCES users(id) ON DELETE SET NULL,
    enrollment_id       INTEGER REFERENCES enrollments(id) ON DELETE SET NULL,
    phone_number        VARCHAR(50) NOT NULL,
    amount              DECIMAL(10, 2) NOT NULL,
    transaction_type    VARCHAR(50),
    transaction_time    TIMESTAMP,
    raw_message         TEXT,
    shortcode           VARCHAR(20),
    account_reference   VARCHAR(100),
    status              VARCHAR(20) DEFAULT 'pending',
    is_processed        BOOLEAN DEFAULT false,
    processed_at        TIMESTAMP,
    processed_by        INTEGER REFERENCES users(id),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- SECTION 14: APPLICATIONS TABLE
-- ============================================================================

CREATE TABLE applications (
    id                  SERIAL PRIMARY KEY,
    user_id             INTEGER REFERENCES users(id) ON DELETE SET NULL,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    phone               VARCHAR(50) NOT NULL,
    date_of_birth       DATE,
    id_number           VARCHAR(50),
    address             TEXT,
    city                VARCHAR(100),
    postal_code         VARCHAR(20),
    location_id         INTEGER REFERENCES locations(id),
    course_id           INTEGER REFERENCES courses(id),
    driving_course      VARCHAR(100),
    computer_course     VARCHAR(100),
    training_location  VARCHAR(255),
    transmission        VARCHAR(50),
    preferred_schedule  DATE,
    driving_experience  VARCHAR(20),
    previous_driving_experience BOOLEAN DEFAULT false,
    emergency_contact_name     VARCHAR(255),
    emergency_contact_phone    VARCHAR(50),
    medical_conditions  TEXT,
    comments            TEXT,
    school_fees         DECIMAL(10, 2),
    fees_paid           DECIMAL(10, 2) DEFAULT 0.00,
    fees_balance        DECIMAL(10, 2),
    payment_status      VARCHAR(20) DEFAULT 'pending',
    status              VARCHAR(20) DEFAULT 'pending',
    verified            VARCHAR(20) DEFAULT 'pending',
    verified_by         INTEGER REFERENCES users(id),
    verified_at         TIMESTAMP,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- SECTION 15: M-PESA MESSAGES TABLE
-- ============================================================================

CREATE TABLE mpesa_messages (
    id                  SERIAL PRIMARY KEY,
    application_id      INTEGER REFERENCES applications(id) ON DELETE SET NULL,
    user_id             INTEGER REFERENCES users(id) ON DELETE SET NULL,
    message             TEXT,
    phone               VARCHAR(50),
    amount              DECIMAL(10, 2),
    mpesa_code          VARCHAR(50),
    status              VARCHAR(20) DEFAULT 'pending',
    verified            BOOLEAN DEFAULT false,
    verified_by         INTEGER REFERENCES users(id),
    verified_at         TIMESTAMP,
    raw_message         TEXT,
    transaction_type    VARCHAR(50),
    transaction_time    TIMESTAMP,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- SECTION 16: CONTACTS TABLE
-- ============================================================================

CREATE TABLE contacts (
    id                  SERIAL PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    email               VARCHAR(255),
    phone               VARCHAR(50),
    subject             VARCHAR(255),
    message             TEXT NOT NULL,
    status              VARCHAR(20) DEFAULT 'new',
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- SECTION 17: SYSTEM SETTINGS TABLE
-- ============================================================================

CREATE TABLE system_settings (
    id                  SERIAL PRIMARY KEY,
    key                 VARCHAR(100) UNIQUE NOT NULL,
    value               TEXT NOT NULL,
    description         TEXT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- SECTION 18: JWT TOKENS TABLE
-- ============================================================================

CREATE TABLE jwt_tokens (
    id                  SERIAL PRIMARY KEY,
    token               VARCHAR(500) UNIQUE NOT NULL,
    user_id             INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at          TIMESTAMP NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- SECTION 19: DEFAULT DATA
-- ============================================================================

-- Default locations
INSERT INTO locations (name, address, phone, email) VALUES
('Githunguri', 'Githunguri Town, Kenya', '+254 790722419', 'githunguri@ultimate.edu'),
('Sunton', 'Sunton Shopping Center, Kenya', '+254 700000000', 'sunton@ultimate.edu');

-- Default departments
INSERT INTO departments (name, description) VALUES
('Driving', 'Defensive driving courses and license training'),
('Computer Studies', 'Computer literacy and professional courses'),
('Administration', 'School administration and management');

-- Default admin user (password: password123)
INSERT INTO users (email, password_hash, first_name, last_name, full_name, phone, role, status, position, location_id, department_id) VALUES
('admin@ultimate.edu', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Admin', 'User', 'Admin User', '+254 790722419', 'admin', 'active', 'System Administrator', 1, 3);

-- Default staff users (password: password123)
INSERT INTO users (email, password_hash, first_name, last_name, full_name, phone, role, status, position, location_id, department_id) VALUES
('staff@ultimate.edu', '6e3f85beeb51e8f6b2153949bae3618388a7456ded7746b04c38b8021d867920', 'Staff', 'Member', 'Staff Member', '+254 700000000', 'staff', 'active', 'Instructor', 1, 1),
('staff2@ultimate.edu', '6e3f85beeb51e8f6b2153949bae3618388a7456ded7746b04c38b8021d867920', 'Second', 'Staff', 'Second Staff', '+254 711111111', 'staff', 'active', 'Instructor', 2, 1);

-- Update location managers
UPDATE locations SET manager_id = 1 WHERE name = 'Githunguri';

-- Update department heads
UPDATE departments SET head_id = 2 WHERE name = 'Driving';

-- System settings
INSERT INTO system_settings (key, value, description) VALUES
('school_name', 'Ultimate Defensive Driving School', 'Name of the school'),
('school_motto', 'Safe Driving for Life', 'School motto'),
('currency', 'KES', 'Currency code'),
('timezone', 'Africa/Nairobi', 'Timezone'),
('academic_year', '2026', 'Current academic year'),
('certificate_validity_years', '3', 'Certificate validity period in years');

-- Default courses
INSERT INTO courses (code, name, description, category, duration_hours, duration_weeks, price, requirements, is_licensed, license_type, is_active, is_featured) VALUES
('CLASS-B', 'Class B Driving License', 'Kenya Class B driving license training for private vehicles', 'Driving', 40, 8, 25000.00, 'Minimum 18 years old, valid ID', true, 'Class B', true, true),
('CLASS-C', 'Class C Driving License', 'Kenya Class C driving license training for commercial vehicles', 'Driving', 60, 12, 35000.00, 'Minimum 20 years old, valid ID, Class B license', true, 'Class C', true, true),
('CLASS-D', 'Class D Driving License', 'Kenya Class D driving license training for motorcycles', 'Driving', 30, 6, 18000.00, 'Minimum 16 years old, valid ID', true, 'Class D', true, false),
('DEF-DRIVE', 'Defensive Driving', 'Advanced defensive driving techniques for safety', 'Driving', 20, 4, 15000.00, 'Valid driving license', false, NULL, true, true),
('COMP-BASIC', 'Computer Basics', 'Introduction to computer operations and Microsoft Office', 'Computer', 30, 6, 12000.00, 'Basic literacy', false, NULL, true, true),
('COMP-ACCT', 'Computer Accounting', 'Sage and QuickBooks accounting software training', 'Computer', 40, 8, 20000.00, 'Basic computer knowledge', false, NULL, true, false),
('GRAPHIC', 'Graphic Design', 'Adobe Photoshop and Illustrator training', 'Computer', 50, 10, 25000.00, 'Basic computer knowledge', false, NULL, true, false);

-- Sample applications with consistent location values
INSERT INTO applications (first_name, last_name, email, phone, date_of_birth, id_number, address, city, postal_code, location_id, course_id, driving_course, computer_course, training_location, transmission, preferred_schedule, previous_driving_experience, emergency_contact_name, emergency_contact_phone, school_fees, fees_paid, fees_balance, payment_status, status) VALUES
('John', 'Mwangi', 'john.mwangi@email.com', '+254 712345678', '1995-03-15', '12345678', '123 Main St', 'Nairobi', '00100', 1, 1, 'Class B', NULL, 'Githunguri', 'Manual', '2026-03-01', false, 'Jane Mwangi', '+254 723456789', 25000.00, 10000.00, 15000.00, 'partial', 'pending'),
('Mary', 'Wanjiku', 'mary.wanjiku@email.com', '+254 723456789', '1998-07-22', '23456789', '456 Oak Ave', 'Kisumu', '40100', 2, 1, 'Class B', NULL, 'Sunton', 'Automatic', '2026-03-15', true, 'Peter Wanjiku', '+254 734567890', 25000.00, 25000.00, 0.00, 'paid', 'approved'),
('David', 'Ochieng', 'david.ochieng@email.com', '+254 734567890', '1990-11-05', '34567890', '789 Pine Rd', 'Mombasa', '80100', 1, 2, 'Class C', NULL, 'Githunguri', 'Manual', '2026-04-01', true, 'Sarah Ochieng', '+254 745678901', 35000.00, 5000.00, 30000.00, 'partial', 'pending'),
('Grace', 'Njoroge', 'grace.njoroge@email.com', '+254 745678901', '2000-01-30', '45678901', '321 Elm St', 'Nairobi', '00200', 1, 5, NULL, 'Computer Basics', 'Githunguri', NULL, '2026-02-15', false, 'James Njoroge', '+254 756789012', 12000.00, 12000.00, 0.00, 'paid', 'approved'),
('Peter', 'Kiprop', 'peter.kiprop@email.com', '+254 756789012', '1992-09-12', '56789012', '654 Maple Ave', 'Eldoret', '30100', 2, 3, 'Class D', NULL, 'Sunton', 'Manual', '2026-03-20', false, 'Mary Kiprop', '+254 767890123', 18000.00, 0.00, 18000.00, 'unpaid', 'pending');

-- ============================================================================
-- SECTION 20: INDEXES
-- ============================================================================

-- Users indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_location ON users(location_id);

-- Courses indexes
CREATE INDEX idx_courses_category ON courses(category);
CREATE INDEX idx_courses_active ON courses(is_active);

-- Classes indexes
CREATE INDEX idx_classes_course ON classes(course_id);
CREATE INDEX idx_classes_status ON classes(status);
CREATE INDEX idx_classes_location ON classes(location_id);

-- Enrollments indexes
CREATE INDEX idx_enrollments_student ON enrollments(student_id);
CREATE INDEX idx_enrollments_class ON enrollments(class_id);
CREATE INDEX idx_enrollments_status ON enrollments(status);
CREATE INDEX idx_enrollments_location ON enrollments(location_id);

-- Attendance indexes
CREATE INDEX idx_attendance_student ON attendance(student_id);
CREATE INDEX idx_attendance_class ON attendance(class_id);
CREATE INDEX idx_attendance_date ON attendance(date);

-- Grades indexes
CREATE INDEX idx_grades_student ON grades(student_id);
CREATE INDEX idx_grades_assessment ON grades(assessment_id);

-- Payments indexes
CREATE INDEX idx_payments_student ON fee_payments(student_id);
CREATE INDEX idx_payments_status ON fee_payments(status);

-- Applications indexes
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_applications_course ON applications(course_id);
CREATE INDEX idx_applications_location ON applications(location_id);

-- M-Pesa messages indexes
CREATE INDEX idx_mpesa_messages_application_id ON mpesa_messages(application_id);
CREATE INDEX idx_mpesa_messages_status ON mpesa_messages(status);

-- JWT tokens indexes
CREATE INDEX idx_jwt_tokens_user_id ON jwt_tokens(user_id);
CREATE INDEX idx_jwt_tokens_expires_at ON jwt_tokens(expires_at);

-- ============================================================================
-- SECTION 21: GRANT PERMISSIONS
-- ============================================================================

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
