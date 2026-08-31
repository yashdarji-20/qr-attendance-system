package com.attendance.dao;

import com.attendance.database.DatabaseConnection;
import com.attendance.model.Attendance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Attendance entity.
 */
public class AttendanceDAO implements GenericDAO<Attendance, Integer> {

    private static final Logger log = LoggerFactory.getLogger(AttendanceDAO.class);
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    private static final String SQL_INSERT =
        "INSERT INTO attendance (student_id, lecture_id, token_id, marked_by, status, remarks, device_info) " +
        "VALUES (?,?,?,?,?,?,?)";

    private static final String SQL_UPDATE =
        "UPDATE attendance SET status=?, remarks=? WHERE attendance_id=?";

    private static final String SQL_DELETE =
        "DELETE FROM attendance WHERE attendance_id=?";

    private static final String SQL_EXISTS =
        "SELECT COUNT(*) FROM attendance WHERE student_id=? AND lecture_id=?";

    private static final String BASE_SELECT =
        "SELECT a.*, " +
        "CONCAT(s.first_name,' ',s.last_name) AS student_name, s.enrollment_no, " +
        "sub.subject_name, sub.subject_code, " +
        "c.class_name, " +
        "DATE_FORMAT(l.lecture_date,'%d-%m-%Y') AS lecture_date, " +
        "TIME_FORMAT(l.start_time,'%H:%i') AS start_time, " +
        "CONCAT(t.first_name,' ',t.last_name) AS teacher_name, " +
        "d.department_name " +
        "FROM attendance a " +
        "JOIN student s   ON a.student_id  = s.student_id " +
        "JOIN lecture l   ON a.lecture_id  = l.lecture_id " +
        "JOIN subject sub ON l.subject_id  = sub.subject_id " +
        "JOIN class   c   ON l.class_id    = c.class_id " +
        "JOIN teacher t   ON l.teacher_id  = t.teacher_id " +
        "JOIN department d ON s.department_id = d.department_id ";

    private static final String SQL_FIND_BY_ID =
        BASE_SELECT + "WHERE a.attendance_id=?";

    private static final String SQL_FIND_BY_STUDENT =
        BASE_SELECT + "WHERE a.student_id=? ORDER BY l.lecture_date DESC, l.start_time DESC";

    private static final String SQL_FIND_BY_LECTURE =
        BASE_SELECT + "WHERE a.lecture_id=? ORDER BY s.enrollment_no";

    private static final String SQL_FIND_BY_STUDENT_SUBJECT =
        BASE_SELECT +
        "WHERE a.student_id=? AND sub.subject_id=? " +
        "ORDER BY l.lecture_date DESC";

    private static final String SQL_FIND_BY_DATE_RANGE =
        BASE_SELECT +
        "WHERE l.class_id=? AND l.lecture_date BETWEEN ? AND ? " +
        "ORDER BY l.lecture_date, s.enrollment_no";

    private static final String SQL_PERCENTAGE =
        "SELECT s.student_id, s.enrollment_no, " +
        "CONCAT(s.first_name,' ',s.last_name) AS student_name, " +
        "COUNT(l.lecture_id) AS total_lectures, " +
        "SUM(CASE WHEN a.status IN ('PRESENT','LATE') THEN 1 ELSE 0 END) AS attended, " +
        "ROUND(SUM(CASE WHEN a.status IN ('PRESENT','LATE') THEN 1 ELSE 0 END)*100.0/" +
        "NULLIF(COUNT(l.lecture_id),0),2) AS percentage " +
        "FROM student s " +
        "JOIN student_class sc ON s.student_id=sc.student_id " +
        "JOIN lecture l ON l.class_id=sc.class_id AND l.subject_id=? " +
        "LEFT JOIN attendance a ON a.student_id=s.student_id AND a.lecture_id=l.lecture_id " +
        "WHERE sc.class_id=? AND s.is_active=TRUE " +
        "GROUP BY s.student_id ORDER BY s.enrollment_no";

    // ---- CRUD --------------------------------------------------------

    @Override
    public int insert(Attendance a) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt    (1, a.getStudentId());
            ps.setInt    (2, a.getLectureId());
            if (a.getTokenId() > 0) ps.setInt(3, a.getTokenId());
            else ps.setNull(3, Types.INTEGER);
            ps.setString (4, a.getMarkedBy().name());
            ps.setString (5, a.getStatus().name());
            ps.setString (6, a.getRemarks());
            ps.setString (7, a.getDeviceInfo());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    a.setAttendanceId(id);
                    log.info("Attendance marked: id={}, student={}, lecture={}",
                            id, a.getStudentId(), a.getLectureId());
                    return id;
                }
            }
        } finally {
            db.releaseConnection(conn);
        }
        return -1;
    }

    @Override
    public int update(Attendance a) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, a.getStatus().name());
            ps.setString(2, a.getRemarks());
            ps.setInt   (3, a.getAttendanceId());
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
    public Optional<Attendance> findById(Integer id) throws SQLException {
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

    @Override
    public List<Attendance> findAll() throws SQLException {
        return findByStudent(0);  // use specific finders
    }

    /** Check if attendance already exists (prevent duplicates) */
    public boolean exists(int studentId, int lectureId) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_EXISTS)) {
            ps.setInt(1, studentId);
            ps.setInt(2, lectureId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } finally {
            db.releaseConnection(conn);
        }
        return false;
    }

    public List<Attendance> findByStudent(int studentId) throws SQLException {
        List<Attendance> list = new ArrayList<>();
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_STUDENT)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public List<Attendance> findByLecture(int lectureId) throws SQLException {
        List<Attendance> list = new ArrayList<>();
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_LECTURE)) {
            ps.setInt(1, lectureId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public List<Attendance> findByStudentAndSubject(int studentId, int subjectId) throws SQLException {
        List<Attendance> list = new ArrayList<>();
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_STUDENT_SUBJECT)) {
            ps.setInt(1, studentId);
            ps.setInt(2, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public List<Attendance> findByDateRange(int classId, Date from, Date to) throws SQLException {
        List<Attendance> list = new ArrayList<>();
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_DATE_RANGE)) {
            ps.setInt (1, classId);
            ps.setDate(2, from);
            ps.setDate(3, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    /**
     * Returns attendance percentage data for all students in a class/subject.
     * Returns rows: student_id, enrollment_no, student_name, total_lectures, attended, percentage
     */
    public List<Object[]> getAttendancePercentage(int subjectId, int classId) throws SQLException {
        List<Object[]> result = new ArrayList<>();
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_PERCENTAGE)) {
            ps.setInt(1, subjectId);
            ps.setInt(2, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Object[]{
                        rs.getInt   ("student_id"),
                        rs.getString("enrollment_no"),
                        rs.getString("student_name"),
                        rs.getInt   ("total_lectures"),
                        rs.getInt   ("attended"),
                        rs.getDouble("percentage")
                    });
                }
            }
        } finally {
            db.releaseConnection(conn);
        }
        return result;
    }

    // ---- Mapper ------------------------------------------------------

    private Attendance map(ResultSet rs) throws SQLException {
        Attendance a = new Attendance();
        a.setAttendanceId(rs.getInt      ("attendance_id"));
        a.setStudentId   (rs.getInt      ("student_id"));
        a.setLectureId   (rs.getInt      ("lecture_id"));
        a.setMarkedAt    (rs.getTimestamp("marked_at"));
        a.setStatus      (Attendance.Status.valueOf(rs.getString("status")));
        a.setRemarks     (rs.getString   ("remarks"));
        // Joined fields
        try {
            a.setStudentName   (rs.getString("student_name"));
            a.setEnrollmentNo  (rs.getString("enrollment_no"));
            a.setSubjectName   (rs.getString("subject_name"));
            a.setSubjectCode   (rs.getString("subject_code"));
            a.setClassName     (rs.getString("class_name"));
            a.setLectureDate   (rs.getString("lecture_date"));
            a.setStartTime     (rs.getString("start_time"));
            a.setTeacherName   (rs.getString("teacher_name"));
            a.setDepartmentName(rs.getString("department_name"));
        } catch (SQLException ignored) {}
        return a;
    }
}
