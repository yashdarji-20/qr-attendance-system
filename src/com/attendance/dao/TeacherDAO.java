package com.attendance.dao;

import com.attendance.database.DatabaseConnection;
import com.attendance.model.Teacher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Teacher entity.
 * All SQL queries for the teacher table live here.
 */
public class TeacherDAO implements GenericDAO<Teacher, Integer> {

    private static final Logger log = LoggerFactory.getLogger(TeacherDAO.class);
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    // ---- SQL Constants -----------------------------------------------

    private static final String SQL_INSERT =
        "INSERT INTO teacher (employee_id, first_name, last_name, email, phone, " +
        "password_hash, department_id, designation, is_active) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
        "UPDATE teacher SET first_name=?, last_name=?, email=?, phone=?, " +
        "department_id=?, designation=?, is_active=? WHERE teacher_id=?";

    private static final String SQL_DELETE =
        "UPDATE teacher SET is_active=FALSE WHERE teacher_id=?";

    private static final String SQL_FIND_BY_ID =
        "SELECT t.*, d.department_name FROM teacher t " +
        "LEFT JOIN department d ON t.department_id=d.department_id " +
        "WHERE t.teacher_id=?";

    private static final String SQL_FIND_BY_EMAIL =
        "SELECT t.*, d.department_name FROM teacher t " +
        "LEFT JOIN department d ON t.department_id=d.department_id " +
        "WHERE t.email=? AND t.is_active=TRUE";

    private static final String SQL_FIND_BY_EMP_ID =
        "SELECT t.*, d.department_name FROM teacher t " +
        "LEFT JOIN department d ON t.department_id=d.department_id " +
        "WHERE t.employee_id=? AND t.is_active=TRUE";

    private static final String SQL_FIND_ALL =
        "SELECT t.*, d.department_name FROM teacher t " +
        "LEFT JOIN department d ON t.department_id=d.department_id " +
        "ORDER BY t.first_name";

    private static final String SQL_UPDATE_LAST_LOGIN =
        "UPDATE teacher SET last_login=NOW() WHERE teacher_id=?";

    private static final String SQL_UPDATE_PASSWORD =
        "UPDATE teacher SET password_hash=? WHERE teacher_id=?";

    // ---- CRUD --------------------------------------------------------

    @Override
    public int insert(Teacher t) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getEmployeeId());
            ps.setString(2, t.getFirstName());
            ps.setString(3, t.getLastName());
            ps.setString(4, t.getEmail());
            ps.setString(5, t.getPhone());
            ps.setString(6, t.getPasswordHash());
            ps.setInt   (7, t.getDepartmentId());
            ps.setString(8, t.getDesignation());
            ps.setBoolean(9, t.isActive());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    t.setTeacherId(id);
                    log.info("Teacher inserted: id={}", id);
                    return id;
                }
            }
        } finally {
            db.releaseConnection(conn);
        }
        return -1;
    }

    @Override
    public int update(Teacher t) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString (1, t.getFirstName());
            ps.setString (2, t.getLastName());
            ps.setString (3, t.getEmail());
            ps.setString (4, t.getPhone());
            ps.setInt    (5, t.getDepartmentId());
            ps.setString (6, t.getDesignation());
            ps.setBoolean(7, t.isActive());
            ps.setInt    (8, t.getTeacherId());
            int rows = ps.executeUpdate();
            log.info("Teacher updated: id={}, rows={}", t.getTeacherId(), rows);
            return rows;
        } finally {
            db.releaseConnection(conn);
        }
    }

    @Override
    public int delete(Integer id) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            log.info("Teacher deactivated: id={}", id);
            return rows;
        } finally {
            db.releaseConnection(conn);
        }
    }

    @Override
    public Optional<Teacher> findById(Integer id) throws SQLException {
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

    public Optional<Teacher> findByEmail(String email) throws SQLException {
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

    public Optional<Teacher> findByEmployeeId(String empId) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_EMP_ID)) {
            ps.setString(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } finally {
            db.releaseConnection(conn);
        }
        return Optional.empty();
    }

    @Override
    public List<Teacher> findAll() throws SQLException {
        List<Teacher> list = new ArrayList<>();
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public void updateLastLogin(int teacherId) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_LAST_LOGIN)) {
            ps.setInt(1, teacherId);
            ps.executeUpdate();
        } finally {
            db.releaseConnection(conn);
        }
    }

    public void updatePassword(int teacherId, String newHash) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PASSWORD)) {
            ps.setString(1, newHash);
            ps.setInt   (2, teacherId);
            ps.executeUpdate();
        } finally {
            db.releaseConnection(conn);
        }
    }

    // ---- ResultSet Mapper --------------------------------------------

    private Teacher map(ResultSet rs) throws SQLException {
        Teacher t = new Teacher();
        t.setTeacherId    (rs.getInt      ("teacher_id"));
        t.setEmployeeId   (rs.getString   ("employee_id"));
        t.setFirstName    (rs.getString   ("first_name"));
        t.setLastName     (rs.getString   ("last_name"));
        t.setEmail        (rs.getString   ("email"));
        t.setPhone        (rs.getString   ("phone"));
        t.setPasswordHash (rs.getString   ("password_hash"));
        t.setDepartmentId (rs.getInt      ("department_id"));
        t.setDesignation  (rs.getString   ("designation"));
        t.setActive       (rs.getBoolean  ("is_active"));
        t.setLastLogin    (rs.getTimestamp("last_login"));
        t.setCreatedAt    (rs.getTimestamp("created_at"));
        try { t.setDepartmentName(rs.getString("department_name")); }
        catch (SQLException ignored) {}
        return t;
    }
}
