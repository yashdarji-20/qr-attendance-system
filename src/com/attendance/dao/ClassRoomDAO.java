package com.attendance.dao;

import com.attendance.database.DatabaseConnection;
import com.attendance.model.ClassRoom;

import java.sql.*;
import java.util.*;

/** DAO for ClassRoom */
public class ClassRoomDAO implements GenericDAO<ClassRoom, Integer> {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    private static final String BASE =
        "SELECT c.*, d.department_name FROM class c " +
        "LEFT JOIN department d ON c.department_id=d.department_id ";

    private static final String SQL_BY_TEACHER =
        BASE + "JOIN teacher_subject ts ON c.class_id=ts.class_id " +
        "WHERE ts.teacher_id=? AND c.is_active=TRUE GROUP BY c.class_id ORDER BY c.class_name";

    @Override public int insert(ClassRoom c) throws SQLException {
        String sql = "INSERT INTO class (class_name, class_code, department_id, semester, year_of_study, room_no, capacity) VALUES (?,?,?,?,?,?,?)";
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getClassName());
            ps.setString(2, c.getClassCode());
            ps.setInt   (3, c.getDepartmentId());
            ps.setInt   (4, c.getSemester());
            ps.setInt   (5, c.getYearOfStudy());
            ps.setString(6, c.getRoomNo());
            ps.setInt   (7, c.getCapacity());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) { c.setClassId(rs.getInt(1)); return c.getClassId(); }
            }
        } finally { db.releaseConnection(conn); }
        return -1;
    }

    @Override public int update(ClassRoom c) throws SQLException { return 0; }
    @Override public int delete(Integer id) throws SQLException { return 0; }

    @Override public Optional<ClassRoom> findById(Integer id) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(BASE + "WHERE c.class_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } finally { db.releaseConnection(conn); }
        return Optional.empty();
    }

    @Override public List<ClassRoom> findAll() throws SQLException {
        List<ClassRoom> list = new ArrayList<>();
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(BASE + "WHERE c.is_active=TRUE ORDER BY c.class_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } finally { db.releaseConnection(conn); }
        return list;
    }

    public List<ClassRoom> findByTeacher(int teacherId) throws SQLException {
        List<ClassRoom> list = new ArrayList<>();
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_BY_TEACHER)) {
            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } finally { db.releaseConnection(conn); }
        return list;
    }

    private ClassRoom map(ResultSet rs) throws SQLException {
        ClassRoom c = new ClassRoom();
        c.setClassId     (rs.getInt    ("class_id"));
        c.setClassName   (rs.getString ("class_name"));
        c.setClassCode   (rs.getString ("class_code"));
        c.setDepartmentId(rs.getInt    ("department_id"));
        c.setSemester    (rs.getInt    ("semester"));
        c.setYearOfStudy (rs.getInt    ("year_of_study"));
        c.setRoomNo      (rs.getString ("room_no"));
        c.setCapacity    (rs.getInt    ("capacity"));
        c.setActive      (rs.getBoolean("is_active"));
        try { c.setDepartmentName(rs.getString("department_name")); } catch (SQLException ignored) {}
        return c;
    }
}
