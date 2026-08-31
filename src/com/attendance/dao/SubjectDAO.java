package com.attendance.dao;

import com.attendance.database.DatabaseConnection;
import com.attendance.model.Subject;

import java.sql.*;
import java.util.*;

/** DAO for Subject */
public class SubjectDAO implements GenericDAO<Subject, Integer> {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    private static final String BASE =
        "SELECT s.*, d.department_name FROM subject s " +
        "LEFT JOIN department d ON s.department_id=d.department_id ";

    private static final String SQL_INSERT =
        "INSERT INTO subject (subject_name, subject_code, department_id, credits, semester, description) " +
        "VALUES (?,?,?,?,?,?)";

    private static final String SQL_UPDATE =
        "UPDATE subject SET subject_name=?, subject_code=?, department_id=?, " +
        "credits=?, semester=?, description=?, is_active=? WHERE subject_id=?";

    private static final String SQL_DELETE =
        "UPDATE subject SET is_active=FALSE WHERE subject_id=?";

    private static final String SQL_BY_TEACHER =
        BASE + "JOIN teacher_subject ts ON s.subject_id=ts.subject_id " +
        "WHERE ts.teacher_id=? AND s.is_active=TRUE GROUP BY s.subject_id";

    @Override
    public int insert(Subject s) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getSubjectName());
            ps.setString(2, s.getSubjectCode());
            ps.setInt   (3, s.getDepartmentId());
            ps.setInt   (4, s.getCredits());
            ps.setInt   (5, s.getSemester());
            ps.setString(6, s.getDescription());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) { s.setSubjectId(rs.getInt(1)); return s.getSubjectId(); }
            }
        } finally { db.releaseConnection(conn); }
        return -1;
    }

    @Override public int update(Subject s) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString (1, s.getSubjectName());
            ps.setString (2, s.getSubjectCode());
            ps.setInt    (3, s.getDepartmentId());
            ps.setInt    (4, s.getCredits());
            ps.setInt    (5, s.getSemester());
            ps.setString (6, s.getDescription());
            ps.setBoolean(7, s.isActive());
            ps.setInt    (8, s.getSubjectId());
            return ps.executeUpdate();
        } finally { db.releaseConnection(conn); }
    }

    @Override public int delete(Integer id) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id); return ps.executeUpdate();
        } finally { db.releaseConnection(conn); }
    }

    @Override public Optional<Subject> findById(Integer id) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(BASE + "WHERE s.subject_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } finally { db.releaseConnection(conn); }
        return Optional.empty();
    }

    @Override public List<Subject> findAll() throws SQLException {
        List<Subject> list = new ArrayList<>();
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(BASE + "WHERE s.is_active=TRUE ORDER BY s.subject_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } finally { db.releaseConnection(conn); }
        return list;
    }

    public List<Subject> findByTeacher(int teacherId) throws SQLException {
        List<Subject> list = new ArrayList<>();
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_BY_TEACHER)) {
            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } finally { db.releaseConnection(conn); }
        return list;
    }

    private Subject map(ResultSet rs) throws SQLException {
        Subject s = new Subject();
        s.setSubjectId   (rs.getInt    ("subject_id"));
        s.setSubjectName (rs.getString ("subject_name"));
        s.setSubjectCode (rs.getString ("subject_code"));
        s.setDepartmentId(rs.getInt    ("department_id"));
        s.setCredits     (rs.getInt    ("credits"));
        s.setSemester    (rs.getInt    ("semester"));
        s.setDescription (rs.getString ("description"));
        s.setActive      (rs.getBoolean("is_active"));
        try { s.setDepartmentName(rs.getString("department_name")); } catch (SQLException ignored) {}
        return s;
    }
}
