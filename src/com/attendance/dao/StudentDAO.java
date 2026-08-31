package com.attendance.dao;

import com.attendance.database.DatabaseConnection;
import com.attendance.model.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Student entity.
 */
public class StudentDAO implements GenericDAO<Student, Integer> {

    private static final Logger log = LoggerFactory.getLogger(StudentDAO.class);
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    private static final String SQL_INSERT =
        "INSERT INTO student (enrollment_no, first_name, last_name, email, phone, " +
        "password_hash, department_id, semester, year_of_study, date_of_birth, address, is_active) " +
        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String SQL_UPDATE =
        "UPDATE student SET first_name=?, last_name=?, email=?, phone=?, department_id=?, " +
        "semester=?, year_of_study=?, date_of_birth=?, address=?, is_active=? WHERE student_id=?";

    private static final String SQL_DELETE =
        "UPDATE student SET is_active=FALSE WHERE student_id=?";

    private static final String BASE_SELECT =
        "SELECT s.*, d.department_name FROM student s " +
        "LEFT JOIN department d ON s.department_id=d.department_id ";

    private static final String SQL_FIND_BY_ID    = BASE_SELECT + "WHERE s.student_id=?";
    private static final String SQL_FIND_BY_EMAIL = BASE_SELECT + "WHERE s.email=? AND s.is_active=TRUE";
    private static final String SQL_FIND_BY_ENROLL= BASE_SELECT + "WHERE s.enrollment_no=? AND s.is_active=TRUE";
    private static final String SQL_FIND_ALL      = BASE_SELECT + "WHERE s.is_active=TRUE ORDER BY s.first_name";

    private static final String SQL_FIND_BY_CLASS =
        BASE_SELECT +
        "JOIN student_class sc ON s.student_id=sc.student_id " +
        "WHERE sc.class_id=? AND sc.is_active=TRUE AND s.is_active=TRUE " +
        "ORDER BY s.enrollment_no";

    private static final String SQL_SEARCH =
        BASE_SELECT +
        "WHERE s.is_active=TRUE AND (" +
        "s.first_name LIKE ? OR s.last_name LIKE ? OR " +
        "s.enrollment_no LIKE ? OR s.email LIKE ?) " +
        "ORDER BY s.first_name";

    private static final String SQL_UPDATE_LAST_LOGIN =
        "UPDATE student SET last_login=NOW() WHERE student_id=?";

    private static final String SQL_UPDATE_PASSWORD =
        "UPDATE student SET password_hash=? WHERE student_id=?";

    private static final String SQL_ENROLL_CLASS =
        "INSERT IGNORE INTO student_class (student_id, class_id) VALUES (?,?)";

    private static final String SQL_UNENROLL_CLASS =
        "UPDATE student_class SET is_active=FALSE WHERE student_id=? AND class_id=?";

    // ---- CRUD --------------------------------------------------------

    @Override
    public int insert(Student s) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString (1,  s.getEnrollmentNo());
            ps.setString (2,  s.getFirstName());
            ps.setString (3,  s.getLastName());
            ps.setString (4,  s.getEmail());
            ps.setString (5,  s.getPhone());
            ps.setString (6,  s.getPasswordHash());
            ps.setInt    (7,  s.getDepartmentId());
            ps.setInt    (8,  s.getSemester());
            ps.setInt    (9,  s.getYearOfStudy());
            ps.setDate   (10, s.getDateOfBirth());
            ps.setString (11, s.getAddress());
            ps.setBoolean(12, s.isActive());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    s.setStudentId(id);
                    log.info("Student inserted: id={}", id);
                    return id;
                }
            }
        } finally {
            db.releaseConnection(conn);
        }
        return -1;
    }

    @Override
    public int update(Student s) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString (1,  s.getFirstName());
            ps.setString (2,  s.getLastName());
            ps.setString (3,  s.getEmail());
            ps.setString (4,  s.getPhone());
            ps.setInt    (5,  s.getDepartmentId());
            ps.setInt    (6,  s.getSemester());
            ps.setInt    (7,  s.getYearOfStudy());
            ps.setDate   (8,  s.getDateOfBirth());
            ps.setString (9,  s.getAddress());
            ps.setBoolean(10, s.isActive());
            ps.setInt    (11, s.getStudentId());
            return ps.executeUpdate();
        } finally {
            db.releaseConnection(conn);
        }
    }

    @Override
    public int delete(Integer id) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        } finally {
            db.releaseConnection(conn);
        }
    }

    @Override
    public Optional<Student> findById(Integer id) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } finally {
            db.releaseConnection(conn);
        }
        return Optional.empty();
    }

    public Optional<Student> findByEmail(String email) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } finally {
            db.releaseConnection(conn);
        }
        return Optional.empty();
    }

    public Optional<Student> findByEnrollmentNo(String enrollNo) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ENROLL)) {
            ps.setString(1, enrollNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } finally {
            db.releaseConnection(conn);
        }
        return Optional.empty();
    }

    @Override
    public List<Student> findAll() throws SQLException {
        List<Student> list = new ArrayList<>();
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public List<Student> findByClass(int classId) throws SQLException {
        List<Student> list = new ArrayList<>();
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_CLASS)) {
            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public List<Student> search(String keyword) throws SQLException {
        List<Student> list = new ArrayList<>();
        String like = "%" + keyword + "%";
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_SEARCH)) {
            for (int i = 1; i <= 4; i++) ps.setString(i, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public void updateLastLogin(int studentId) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_LAST_LOGIN)) {
            ps.setInt(1, studentId);
            ps.executeUpdate();
        } finally {
            db.releaseConnection(conn);
        }
    }

    public void updatePassword(int studentId, String newHash) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PASSWORD)) {
            ps.setString(1, newHash);
            ps.setInt   (2, studentId);
            ps.executeUpdate();
        } finally {
            db.releaseConnection(conn);
        }
    }

    public void enrollInClass(int studentId, int classId) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_ENROLL_CLASS)) {
            ps.setInt(1, studentId);
            ps.setInt(2, classId);
            ps.executeUpdate();
        } finally {
            db.releaseConnection(conn);
        }
    }

    public void unenrollFromClass(int studentId, int classId) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_UNENROLL_CLASS)) {
            ps.setInt(1, studentId);
            ps.setInt(2, classId);
            ps.executeUpdate();
        } finally {
            db.releaseConnection(conn);
        }
    }

    // ---- ResultSet Mapper --------------------------------------------

    private Student map(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setStudentId    (rs.getInt      ("student_id"));
        s.setEnrollmentNo (rs.getString   ("enrollment_no"));
        s.setFirstName    (rs.getString   ("first_name"));
        s.setLastName     (rs.getString   ("last_name"));
        s.setEmail        (rs.getString   ("email"));
        s.setPhone        (rs.getString   ("phone"));
        s.setPasswordHash (rs.getString   ("password_hash"));
        s.setDepartmentId (rs.getInt      ("department_id"));
        s.setSemester     (rs.getInt      ("semester"));
        s.setYearOfStudy  (rs.getInt      ("year_of_study"));
        s.setDateOfBirth  (rs.getDate     ("date_of_birth"));
        s.setAddress      (rs.getString   ("address"));
        s.setActive       (rs.getBoolean  ("is_active"));
        s.setLastLogin    (rs.getTimestamp("last_login"));
        s.setCreatedAt    (rs.getTimestamp("created_at"));
        try { s.setDepartmentName(rs.getString("department_name")); }
        catch (SQLException ignored) {}
        return s;
    }
}
