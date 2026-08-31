package com.attendance.dao;

import com.attendance.database.DatabaseConnection;
import com.attendance.model.Department;

import java.sql.*;
import java.util.*;

/** DAO for Department */
public class DepartmentDAO implements GenericDAO<Department, Integer> {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override public int insert(Department d) throws SQLException {
        String sql = "INSERT INTO department (department_name, department_code, description) VALUES (?,?,?)";
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getDepartmentName());
            ps.setString(2, d.getDepartmentCode());
            ps.setString(3, d.getDescription());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) { d.setDepartmentId(rs.getInt(1)); return d.getDepartmentId(); }
            }
        } finally { db.releaseConnection(conn); }
        return -1;
    }

    @Override public int update(Department d) throws SQLException { return 0; }

    @Override public int delete(Integer id) throws SQLException {
        String sql = "UPDATE department SET is_active=FALSE WHERE department_id=?";
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id); return ps.executeUpdate();
        } finally { db.releaseConnection(conn); }
    }

    @Override public Optional<Department> findById(Integer id) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM department WHERE department_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } finally { db.releaseConnection(conn); }
        return Optional.empty();
    }

    @Override public List<Department> findAll() throws SQLException {
        List<Department> list = new ArrayList<>();
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM department WHERE is_active=TRUE ORDER BY department_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } finally { db.releaseConnection(conn); }
        return list;
    }

    private Department map(ResultSet rs) throws SQLException {
        Department d = new Department();
        d.setDepartmentId  (rs.getInt    ("department_id"));
        d.setDepartmentName(rs.getString ("department_name"));
        d.setDepartmentCode(rs.getString ("department_code"));
        d.setDescription   (rs.getString ("description"));
        d.setActive        (rs.getBoolean("is_active"));
        return d;
    }
}
