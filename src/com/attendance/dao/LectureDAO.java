package com.attendance.dao;

import com.attendance.database.DatabaseConnection;
import com.attendance.model.Lecture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO for Lecture entity */
public class LectureDAO implements GenericDAO<Lecture, Integer> {

    private static final Logger log = LoggerFactory.getLogger(LectureDAO.class);
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    private static final String SQL_INSERT =
        "INSERT INTO lecture (subject_id, teacher_id, class_id, lecture_date, start_time, " +
        "end_time, topic, lecture_type, status) VALUES (?,?,?,?,?,?,?,?,?)";

    private static final String SQL_UPDATE =
        "UPDATE lecture SET status=?, end_time=?, topic=?, notes=? WHERE lecture_id=?";

    private static final String SQL_UPDATE_STATUS =
        "UPDATE lecture SET status=? WHERE lecture_id=?";

    private static final String SQL_DELETE = "DELETE FROM lecture WHERE lecture_id=?";

    private static final String BASE_SELECT =
        "SELECT l.*, sub.subject_name, sub.subject_code, " +
        "CONCAT(t.first_name,' ',t.last_name) AS teacher_name, " +
        "c.class_name, " +
        "(SELECT COUNT(*) FROM attendance a WHERE a.lecture_id=l.lecture_id AND a.status='PRESENT') AS present_count, " +
        "(SELECT COUNT(*) FROM student_class sc WHERE sc.class_id=l.class_id AND sc.is_active=TRUE) AS total_students " +
        "FROM lecture l " +
        "JOIN subject sub ON l.subject_id=sub.subject_id " +
        "JOIN teacher t   ON l.teacher_id=t.teacher_id " +
        "JOIN class   c   ON l.class_id=c.class_id ";

    private static final String SQL_FIND_BY_ID =
        BASE_SELECT + "WHERE l.lecture_id=?";

    private static final String SQL_FIND_BY_TEACHER =
        BASE_SELECT + "WHERE l.teacher_id=? ORDER BY l.lecture_date DESC, l.start_time DESC";

    private static final String SQL_FIND_BY_CLASS =
        BASE_SELECT + "WHERE l.class_id=? ORDER BY l.lecture_date DESC, l.start_time DESC";

    private static final String SQL_FIND_BY_CLASS_SUBJECT =
        BASE_SELECT + "WHERE l.class_id=? AND l.subject_id=? ORDER BY l.lecture_date DESC";

    private static final String SQL_FIND_TODAY =
        BASE_SELECT + "WHERE l.teacher_id=? AND l.lecture_date=CURDATE() ORDER BY l.start_time";

    private static final String SQL_FIND_ALL =
        BASE_SELECT + "ORDER BY l.lecture_date DESC, l.start_time DESC LIMIT 200";

    // ---- CRUD --------------------------------------------------------

    @Override
    public int insert(Lecture l) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, l.getSubjectId());
            ps.setInt   (2, l.getTeacherId());
            ps.setInt   (3, l.getClassId());
            ps.setDate  (4, l.getLectureDate());
            ps.setTime  (5, l.getStartTime());
            if (l.getEndTime() != null) ps.setTime(6, l.getEndTime());
            else ps.setNull(6, Types.TIME);
            ps.setString(7, l.getTopic());
            ps.setString(8, l.getLectureType().name());
            ps.setString(9, l.getStatus().name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    l.setLectureId(id);
                    return id;
                }
            }
        } finally {
            db.releaseConnection(conn);
        }
        return -1;
    }

    @Override
    public int update(Lecture l) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, l.getStatus().name());
            if (l.getEndTime() != null) ps.setTime(2, l.getEndTime());
            else ps.setNull(2, Types.TIME);
            ps.setString(3, l.getTopic());
            ps.setString(4, l.getNotes());
            ps.setInt   (5, l.getLectureId());
            return ps.executeUpdate();
        } finally {
            db.releaseConnection(conn);
        }
    }

    public int updateStatus(int lectureId, Lecture.LectureStatus status) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {
            ps.setString(1, status.name());
            ps.setInt   (2, lectureId);
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
    public Optional<Lecture> findById(Integer id) throws SQLException {
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

    public List<Lecture> findByTeacher(int teacherId) throws SQLException {
        return queryList(SQL_FIND_BY_TEACHER, teacherId);
    }

    public List<Lecture> findByClass(int classId) throws SQLException {
        return queryList(SQL_FIND_BY_CLASS, classId);
    }

    public List<Lecture> findByClassAndSubject(int classId, int subjectId) throws SQLException {
        List<Lecture> list = new ArrayList<>();
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_CLASS_SUBJECT)) {
            ps.setInt(1, classId);
            ps.setInt(2, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public List<Lecture> findTodayByTeacher(int teacherId) throws SQLException {
        return queryList(SQL_FIND_TODAY, teacherId);
    }

    @Override
    public List<Lecture> findAll() throws SQLException {
        List<Lecture> list = new ArrayList<>();
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    // ---- Helpers -----------------------------------------------------

    private List<Lecture> queryList(String sql, int param) throws SQLException {
        List<Lecture> list = new ArrayList<>();
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    private Lecture map(ResultSet rs) throws SQLException {
        Lecture l = new Lecture();
        l.setLectureId  (rs.getInt   ("lecture_id"));
        l.setSubjectId  (rs.getInt   ("subject_id"));
        l.setTeacherId  (rs.getInt   ("teacher_id"));
        l.setClassId    (rs.getInt   ("class_id"));
        l.setLectureDate(rs.getDate  ("lecture_date"));
        l.setStartTime  (rs.getTime  ("start_time"));
        l.setEndTime    (rs.getTime  ("end_time"));
        l.setTopic      (rs.getString("topic"));
        l.setNotes      (rs.getString("notes"));
        l.setStatus     (Lecture.LectureStatus.valueOf(rs.getString("status")));
        l.setLectureType(Lecture.LectureType.valueOf(rs.getString("lecture_type")));
        l.setCreatedAt  (rs.getTimestamp("created_at"));
        try {
            l.setSubjectName  (rs.getString("subject_name"));
            l.setSubjectCode  (rs.getString("subject_code"));
            l.setTeacherName  (rs.getString("teacher_name"));
            l.setClassName    (rs.getString("class_name"));
            l.setPresentCount (rs.getInt   ("present_count"));
            l.setTotalStudents(rs.getInt   ("total_students"));
        } catch (SQLException ignored) {}
        return l;
    }
}
