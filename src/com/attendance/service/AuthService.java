package com.attendance.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.attendance.dao.StudentDAO;
import com.attendance.dao.TeacherDAO;
import com.attendance.model.Student;
import com.attendance.model.Teacher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Service handling authentication for both Teacher and Student.
 * Uses BCrypt for secure password hashing.
 *
 * Default credentials (see schema.sql seed data):
 *   Teacher: empId=EMP001, password=Teacher@123
 *   Student: enrollNo=2021CSE001, password=Student@123
 */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final TeacherDAO teacherDAO;
    private final StudentDAO studentDAO;

    // Session holders (singleton pattern for current session)
    private static Teacher currentTeacher;
    private static Student currentStudent;

    public AuthService() {
        this.teacherDAO = new TeacherDAO();
        this.studentDAO = new StudentDAO();
    }

    // ---- Teacher Authentication --------------------------------------

    /**
     * Authenticate a teacher by employee ID or email + password.
     *
     * @param identifier employeeId or email
     * @param password   plain-text password
     * @return LoginResult with success flag and message
     */
    public LoginResult loginTeacher(String identifier, String password) {
        if (identifier == null || identifier.isBlank() ||
            password   == null || password.isBlank()) {
            return new LoginResult(false, null, "Please enter your credentials.");
        }

        try {
            Optional<Teacher> opt;
            // Try by email first, then by employee ID
            if (identifier.contains("@")) {
                opt = teacherDAO.findByEmail(identifier.trim().toLowerCase());
            } else {
                opt = teacherDAO.findByEmployeeId(identifier.trim().toUpperCase());
            }

            if (opt.isEmpty()) {
                return new LoginResult(false, null, "Invalid credentials. Teacher not found.");
            }

            Teacher teacher = opt.get();

            if (!teacher.isActive()) {
                return new LoginResult(false, null, "Your account has been deactivated.");
            }

            if (!verifyPassword(password, teacher.getPasswordHash())) {
                log.warn("Failed login attempt for teacher: {}", identifier);
                return new LoginResult(false, null, "Incorrect password.");
            }

            // Update last login
            teacherDAO.updateLastLogin(teacher.getTeacherId());
            currentTeacher = teacher;
            log.info("Teacher logged in: {}", teacher.getFullName());
            return new LoginResult(true, teacher, "Welcome back, " + teacher.getFirstName() + "!");

        } catch (SQLException e) {
            log.error("Teacher login error: {}", e.getMessage());
            return new LoginResult(false, null, "Database error. Please try again.");
        }
    }

    /**
     * Authenticate a student by enrollment number or email + password.
     */
    public LoginResult loginStudent(String identifier, String password) {
        if (identifier == null || identifier.isBlank() ||
            password   == null || password.isBlank()) {
            return new LoginResult(false, null, "Please enter your credentials.");
        }

        try {
            Optional<Student> opt;
            if (identifier.contains("@")) {
                opt = studentDAO.findByEmail(identifier.trim().toLowerCase());
            } else {
                opt = studentDAO.findByEnrollmentNo(identifier.trim().toUpperCase());
            }

            if (opt.isEmpty()) {
                return new LoginResult(false, null, "Invalid credentials. Student not found.");
            }

            Student student = opt.get();

            if (!student.isActive()) {
                return new LoginResult(false, null, "Your account has been deactivated.");
            }

            if (!verifyPassword(password, student.getPasswordHash())) {
                log.warn("Failed login attempt for student: {}", identifier);
                return new LoginResult(false, null, "Incorrect password.");
            }

            studentDAO.updateLastLogin(student.getStudentId());
            currentStudent = student;
            log.info("Student logged in: {}", student.getFullName());
            return new LoginResult(true, student, "Welcome back, " + student.getFirstName() + "!");

        } catch (SQLException e) {
            log.error("Student login error: {}", e.getMessage());
            return new LoginResult(false, null, "Database error. Please try again.");
        }
    }

    // ---- Session Management ------------------------------------------

    public static Teacher getCurrentTeacher()       { return currentTeacher; }
    public static Student getCurrentStudent()       { return currentStudent; }
    public static boolean isTeacherLoggedIn()       { return currentTeacher != null; }
    public static boolean isStudentLoggedIn()       { return currentStudent != null; }

    public void logoutTeacher() {
        log.info("Teacher logged out: {}",
                currentTeacher != null ? currentTeacher.getFullName() : "none");
        currentTeacher = null;
    }

    public void logoutStudent() {
        log.info("Student logged out: {}",
                currentStudent != null ? currentStudent.getFullName() : "none");
        currentStudent = null;
    }

    // ---- Password Management -----------------------------------------

    /**
     * Hash a plain-text password using BCrypt.
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.withDefaults().hashToString(12, plainPassword.toCharArray());
    }

    /**
     * Verify a plain-text password against a BCrypt hash.
     */
    public static boolean verifyPassword(String plainPassword, String hash) {
        if (plainPassword == null || hash == null) return false;
        try {
            BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), hash);
            return result.verified;
        } catch (Exception e) {
            log.error("Password verification error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Change a teacher's password after verifying the old one.
     */
    public boolean changeTeacherPassword(int teacherId, String oldPassword, String newPassword)
            throws SQLException {
        Optional<Teacher> opt = teacherDAO.findById(teacherId);
        if (opt.isEmpty()) return false;

        Teacher teacher = opt.get();
        if (!verifyPassword(oldPassword, teacher.getPasswordHash())) {
            return false;
        }
        String newHash = hashPassword(newPassword);
        teacherDAO.updatePassword(teacherId, newHash);
        return true;
    }

    /**
     * Change a student's password after verifying the old one.
     */
    public boolean changeStudentPassword(int studentId, String oldPassword, String newPassword)
            throws SQLException {
        Optional<Student> opt = studentDAO.findById(studentId);
        if (opt.isEmpty()) return false;

        Student student = opt.get();
        if (!verifyPassword(oldPassword, student.getPasswordHash())) {
            return false;
        }
        String newHash = hashPassword(newPassword);
        studentDAO.updatePassword(studentId, newHash);
        return true;
    }

    // ---- Nested Result Class -----------------------------------------

    /**
     * Encapsulates the result of a login attempt.
     */
    public static final class LoginResult {
        private final boolean success;
        private final Object  user;       // Teacher or Student
        private final String  message;

        public LoginResult(boolean success, Object user, String message) {
            this.success = success;
            this.user    = user;
            this.message = message;
        }

        public boolean isSuccess()   { return success; }
        public Object  getUser()     { return user; }
        public String  getMessage()  { return message; }
        public Teacher getTeacher()  { return (Teacher) user; }
        public Student getStudent()  { return (Student) user; }
    }
}
