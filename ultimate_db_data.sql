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
(3, 'motiv.mot@ultimat.udd', '39fbaa5f806c772ca53ddfc3bf4419faa9894336c08787bd3ec28fd1e97ec8bb', 'Timothy', 'Munyiri', 'Timothy ', '0790722419', 'user', 'active', NULL, 1, NULL, 'Sunton - Sunton Shopping Center, Nairobi'),
(4, 'john@gmial.com', '96d9632f363564cc3032521409cf22a852f2032eec099ed5967c0d000cec607a', 'John', 'Maina', 'John Main', '0790722419', 'applicant', 'active', NULL, 7, NULL, 'Sunton - Sunton Shopping Center, Nairobi'),
(5, 'mark@gmail.com', '6201eb4dccc956cc4fa3a78dca0c2888177ec52efd48f125df214f046eb43138', 'Mark', 'Waweru', 'Mark Waweru', '0790000000', 'applicant', 'active', NULL, 7, NULL, 'Sunton - Sunton Shopping Center, Nairobi');

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
(15, 'ADD-03', 'Road Safety Awareness', 'Essential road safety knowledge.', 'Defensive', 15, NULL, 8000.00, 'None', true),
(16, 'CRS1774030234484', 'Computer ', '', 'general', 20, NULL, 10000.00, 'None', true),
(17, 'CRS1774033373059', 'Assessment', 'good Work', 'driving', 20, 0, 15000.00, 'NationalID, School ID', true);

-- ============================================================================
-- APPLICATIONS (only columns that are saved)
-- ============================================================================
INSERT INTO applications (id, user_id, first_name, last_name, email, phone, date_of_birth, id_number, address, city, postal_code, course_id, license_type, driving_course, computer_course, location_id, training_location, transmission, preferred_schedule, preferred_start, previous_driving_experience, emergency_contact_name, emergency_contact_phone, comments, medical_conditions, status, school_fees, fees_paid, fees_balance, payment_status) VALUES 
(13, 3, 'Timothy', 'Munyiri', 'motiv.mot@ultimat.udd', '0790722419', '2001-05-19', '38479955', '127', 'Nairobi', '10100', 1, 'Class B', 'Class B License', 'Computer Basics', 2, 'Sunton', 'Both', 'Morning', '2026-02-15', true, 'Timothy Munyiri', '0790722419', 'No comment', NULL, 'approved', 15000.00, 10000.00, 5000.00, 'partial'),
(14, 3, 'Timothy', 'Kamau', 'motiv.mot@ultimat.udd', '0790722419', '2001-05-19', '38479955', '127', 'Nairobi', '10100', 1, 'Class B', 'Class C License', '', 2, 'Sunton', 'Manual', 'Morning', '2026-02-15', false, 'Timothy Munyiri', '0790722419', 'No Comments', NULL, 'approved', 23000.00, 17000.00, 6000.00, 'partial'),
(15, 4, 'John', 'Maina', 'john@gmial.com', '0790722419', '2001-05-19', '38479955', '', 'Nairobi', '101100', 14, 'Class B', 'Class B License', '', 7, 'Zimmerman Branch', 'Both', 'Morning', '2026-02-17', false, '', '', '', NULL, 'approved', 15000.00, 10000.00, 5000.00, 'partial'),
(16, 5, 'Mark', 'Waweru', 'mark@gmail.com', '0790000000', '2001-05-19', '356479698', '127', 'Nairobi', '10100', NULL, 'Class B', 'Class B License', '', 2, 'Sunton', 'Both', 'Morning', '2026-02-23', true, 'Timothy Munyiri', '0790722419', 'Not yet', NULL, 'approved', 25000.00, 10000.00, 15000.00, 'partial');

-- ============================================================================
-- CLASSES
-- ============================================================================
INSERT INTO classes (id, course_id, name, code, description, location_id, start_date, end_date, start_time, end_time, days_of_week, instructor_id, max_students, current_students, status, class_fee) VALUES 
(1, 1, 'Class B License - Morning', 'CLS-B-001', 'Light vehicle driving course for cars and small trucks - Morning session', 2, '2026-03-01', '2026-04-30', '08:00', '12:00', 'Mon,Wed,Fri', 6, 20, 5, 'open', 15000.00),
(2, 1, 'Class B License - Afternoon', 'CLS-B-002', 'Light vehicle driving course - Afternoon session', 2, '2026-03-01', '2026-04-30', '14:00', '18:00', 'Tue,Thu,Sat', 6, 20, 3, 'open', 15000.00),
(3, 2, 'Class C License - Full Day', 'CLS-C-001', 'Heavy commercial vehicle driving course', 2, '2026-03-15', '2026-05-15', '08:00', '17:00', 'Mon,Tue,Wed', 6, 15, 2, 'open', 25000.00),
(4, 5, 'Defensive Driving Course', 'CLS-DEF-001', 'Advanced driving techniques for experienced drivers', 7, '2026-04-01', '2026-04-20', '09:00', '13:00', 'Sat', 6, 25, 8, 'open', 10000.00),
(5, 8, 'Computer Accounting - Evening', 'CLS-ACC-001', 'Accounting software training - Evening classes', 2, '2026-03-10', '2026-05-10', '18:00', '21:00', 'Mon,Wed', 7, 20, 12, 'open', 10000.00);

-- ============================================================================
-- ENROLLMENTS
-- ============================================================================
INSERT INTO enrollments (id, student_id, class_id, enrollment_number, enrollment_date, status, progress_percentage, fee_amount, fee_paid, fee_balance, payment_status, license_type, driving_course, computer_course, transmission, preferred_schedule, training_location) VALUES 
(1, 3, 1, 'ENR-2026-001', '2026-03-05', 'enrolled', 25.00, 15000.00, 10000.00, 5000.00, 'partial', 'Class B', 'Defensive Driving', '', 'Automatic', 'Weekday', 'Nairobi'),
(2, 3, 4, 'ENR-2026-002', '2026-04-01', 'enrolled', 10.00, 10000.00, 10000.00, 0.00, 'paid', 'Class C', 'Computer Basics', '', 'Manual', 'Evening', 'Mombasa'),
(3, 4, 1, 'ENR-2026-003', '2026-03-10', 'enrolled', 15.00, 15000.00, 5000.00, 10000.00, 'partial', 'Class B', 'Defensive Driving', 'Graphic Design', 'Automatic', 'Weekend', 'Kisumu'),
(4, 5, 2, 'ENR-2026-004', '2026-03-12', 'enrolled', 20.00, 15000.00, 15000.00, 0.00, 'paid', 'Class D', 'Refresher Course', '', 'Automatic', 'Weekday', 'Nairobi');
