-- ============================================================
-- QR Code Based Attendance Management System
-- DY PATIL SCHOOL OF BIOTECHNOLOGY AND BIOINFORMATICS
-- Complete Schema + Seed Data
-- ============================================================

CREATE DATABASE IF NOT EXISTS attendance_system
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE attendance_system;

SET FOREIGN_KEY_CHECKS = 0;

-- ---- Tables ----

CREATE TABLE IF NOT EXISTS department (
    department_id   INT AUTO_INCREMENT PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL,
    department_code VARCHAR(20)  NOT NULL UNIQUE,
    description     TEXT,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS teacher (
    teacher_id      INT AUTO_INCREMENT PRIMARY KEY,
    employee_id     VARCHAR(20)  NOT NULL UNIQUE,
    first_name      VARCHAR(50)  NOT NULL,
    last_name       VARCHAR(50)  NOT NULL,
    email           VARCHAR(100) NOT NULL UNIQUE,
    phone           VARCHAR(15),
    password_hash   VARCHAR(255) NOT NULL,
    department_id   INT,
    designation     VARCHAR(100),
    is_active       BOOLEAN DEFAULT TRUE,
    last_login      TIMESTAMP NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES department(department_id)
);

CREATE TABLE IF NOT EXISTS student (
    student_id      INT AUTO_INCREMENT PRIMARY KEY,
    enrollment_no   VARCHAR(30)  NOT NULL UNIQUE,
    first_name      VARCHAR(50)  NOT NULL,
    last_name       VARCHAR(50)  NOT NULL,
    email           VARCHAR(100) NOT NULL UNIQUE,
    phone           VARCHAR(15),
    password_hash   VARCHAR(255) NOT NULL,
    department_id   INT,
    semester        INT DEFAULT 1,
    year_of_study   INT DEFAULT 1,
    date_of_birth   DATE,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES department(department_id)
);

CREATE TABLE IF NOT EXISTS class (
    class_id        INT AUTO_INCREMENT PRIMARY KEY,
    class_name      VARCHAR(50)  NOT NULL,
    class_code      VARCHAR(20)  NOT NULL UNIQUE,
    department_id   INT,
    semester        INT,
    year_of_study   INT,
    room_no         VARCHAR(20),
    capacity        INT DEFAULT 60,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES department(department_id)
);

CREATE TABLE IF NOT EXISTS student_class (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    student_id      INT NOT NULL,
    class_id        INT NOT NULL,
    enrolled_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active       BOOLEAN DEFAULT TRUE,
    UNIQUE KEY uq_student_class (student_id, class_id),
    FOREIGN KEY (student_id) REFERENCES student(student_id) ON DELETE CASCADE,
    FOREIGN KEY (class_id)   REFERENCES class(class_id)   ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS subject (
    subject_id      INT AUTO_INCREMENT PRIMARY KEY,
    subject_name    VARCHAR(100) NOT NULL,
    subject_code    VARCHAR(20)  NOT NULL UNIQUE,
    department_id   INT,
    credits         INT DEFAULT 4,
    semester        INT,
    description     TEXT,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES department(department_id)
);

CREATE TABLE IF NOT EXISTS teacher_subject (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    teacher_id      INT NOT NULL,
    subject_id      INT NOT NULL,
    class_id        INT NOT NULL,
    academic_year   VARCHAR(20),
    UNIQUE KEY uq_teacher_subject_class (teacher_id, subject_id, class_id),
    FOREIGN KEY (teacher_id) REFERENCES teacher(teacher_id)  ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subject(subject_id)  ON DELETE CASCADE,
    FOREIGN KEY (class_id)   REFERENCES class(class_id)      ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS lecture (
    lecture_id      INT AUTO_INCREMENT PRIMARY KEY,
    subject_id      INT NOT NULL,
    teacher_id      INT NOT NULL,
    class_id        INT NOT NULL,
    lecture_date    DATE NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME,
    topic           VARCHAR(200),
    lecture_type    ENUM('THEORY','PRACTICAL','TUTORIAL') DEFAULT 'THEORY',
    status          ENUM('SCHEDULED','ONGOING','COMPLETED','CANCELLED') DEFAULT 'SCHEDULED',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subject(subject_id),
    FOREIGN KEY (teacher_id) REFERENCES teacher(teacher_id),
    FOREIGN KEY (class_id)   REFERENCES class(class_id)
);

CREATE TABLE IF NOT EXISTS qr_token (
    token_id        INT AUTO_INCREMENT PRIMARY KEY,
    lecture_id      INT NOT NULL,
    token_value     VARCHAR(64) NOT NULL UNIQUE,
    qr_data         TEXT,
    generated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP NOT NULL,
    is_expired      BOOLEAN DEFAULT FALSE,
    scan_count      INT DEFAULT 0,
    FOREIGN KEY (lecture_id) REFERENCES lecture(lecture_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS attendance (
    attendance_id   INT AUTO_INCREMENT PRIMARY KEY,
    student_id      INT NOT NULL,
    lecture_id      INT NOT NULL,
    token_id        INT,
    marked_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status          ENUM('PRESENT','ABSENT','LATE','EXCUSED') DEFAULT 'PRESENT',
    marked_by       ENUM('QR','MANUAL','SYSTEM') DEFAULT 'QR',
    remarks         TEXT,
    UNIQUE KEY uq_student_lecture (student_id, lecture_id),
    FOREIGN KEY (student_id) REFERENCES student(student_id) ON DELETE CASCADE,
    FOREIGN KEY (lecture_id) REFERENCES lecture(lecture_id) ON DELETE CASCADE,
    FOREIGN KEY (token_id)   REFERENCES qr_token(token_id)
);

CREATE TABLE IF NOT EXISTS attendance_summary (
    summary_id      INT AUTO_INCREMENT PRIMARY KEY,
    student_id      INT NOT NULL,
    subject_id      INT NOT NULL,
    class_id        INT NOT NULL,
    total_lectures  INT DEFAULT 0,
    attended        INT DEFAULT 0,
    percentage      DECIMAL(5,2) DEFAULT 0.00,
    last_updated    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_summary (student_id, subject_id, class_id),
    FOREIGN KEY (student_id) REFERENCES student(student_id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subject(subject_id) ON DELETE CASCADE,
    FOREIGN KEY (class_id)   REFERENCES class(class_id)     ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS system_config (
    config_id       INT AUTO_INCREMENT PRIMARY KEY,
    config_key      VARCHAR(100) NOT NULL UNIQUE,
    config_value    TEXT,
    description     TEXT,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_log (
    log_id          INT AUTO_INCREMENT PRIMARY KEY,
    user_type       ENUM('TEACHER','STUDENT','SYSTEM'),
    user_id         INT,
    action          VARCHAR(100),
    details         TEXT,
    ip_address      VARCHAR(50),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- SEED DATA
-- ============================================================

-- Departments
INSERT INTO department (department_id, department_name, department_code, is_active) VALUES
(1, 'Bioinformatics',  'BID', TRUE),
(2, 'Biotechnology',   'BBT', TRUE),
(3, 'Food Technology', 'BFT', TRUE)
ON DUPLICATE KEY UPDATE department_name=VALUES(department_name);

-- Teacher: Monali Mam
-- Password: Teacher@123
INSERT INTO teacher (teacher_id, employee_id, first_name, last_name, email, phone, password_hash, department_id, designation) VALUES
(1, 'EMP001', 'Monali', 'Mam', 'Monali@dypatil.edu', '9876543210',
 '$2a$12$et76ls4zqiOOfAtZ3YXmY.A2vfs9.tKUzCP8qAHocffuBsc42V/2m', 1, 'Professor')
ON DUPLICATE KEY UPDATE
    first_name=VALUES(first_name), last_name=VALUES(last_name),
    email=VALUES(email), password_hash=VALUES(password_hash),
    designation=VALUES(designation);

-- Subject
INSERT INTO subject (subject_id, subject_name, subject_code, department_id, credits, semester, is_active) VALUES
(1, 'Java and Biojava', '2403BIC5T4', 1, 4, 5, TRUE)
ON DUPLICATE KEY UPDATE subject_name=VALUES(subject_name), subject_code=VALUES(subject_code);

-- Class
INSERT INTO class (class_id, class_name, class_code, department_id, semester, year_of_study, capacity, is_active) VALUES
(1, 'Sem-5', 'SEM5', 1, 5, 3, 60, TRUE)
ON DUPLICATE KEY UPDATE class_name=VALUES(class_name), semester=VALUES(semester);

-- Link teacher → subject → class
INSERT INTO teacher_subject (teacher_id, subject_id, class_id, academic_year) VALUES
(1, 1, 1, '2024-2025')
ON DUPLICATE KEY UPDATE academic_year=VALUES(academic_year);

-- System config
INSERT INTO system_config (config_key, config_value, description) VALUES
('college_name',    'DY PATIL SCHOOL OF BIOTECHNOLOGY AND BIOINFORMATICS', 'College name'),
('qr_expiry_secs',  '30',   'QR code expiry in seconds'),
('min_attendance',  '75',   'Minimum attendance percentage required'),
('academic_year',   '2024-2025', 'Current academic year')
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value);

-- ============================================================
-- DONE — No sample students (add via app)
-- ============================================================
