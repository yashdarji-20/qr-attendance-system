package com.attendance.dao;

import com.attendance.database.DatabaseConnection;
import com.attendance.model.QRToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO for QR Token entity */
public class QRTokenDAO implements GenericDAO<QRToken, Integer> {

    private static final Logger log = LoggerFactory.getLogger(QRTokenDAO.class);
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    private static final String SQL_INSERT =
        "INSERT INTO qr_token (lecture_id, token_value, qr_data, generated_at, expires_at) " +
        "VALUES (?,?,?,NOW(),?)";

    private static final String SQL_FIND_BY_ID =
        "SELECT q.*, l.subject_id, l.teacher_id, l.class_id, " +
        "sub.subject_name, CONCAT(t.first_name,' ',t.last_name) AS teacher_name " +
        "FROM qr_token q " +
        "JOIN lecture l   ON q.lecture_id  = l.lecture_id " +
        "JOIN subject sub ON l.subject_id  = sub.subject_id " +
        "JOIN teacher t   ON l.teacher_id  = t.teacher_id " +
        "WHERE q.token_id=?";

    private static final String SQL_FIND_BY_TOKEN =
        "SELECT q.*, l.subject_id, l.teacher_id, l.class_id, " +
        "sub.subject_name, CONCAT(t.first_name,' ',t.last_name) AS teacher_name " +
        "FROM qr_token q " +
        "JOIN lecture l   ON q.lecture_id  = l.lecture_id " +
        "JOIN subject sub ON l.subject_id  = sub.subject_id " +
        "JOIN teacher t   ON l.teacher_id  = t.teacher_id " +
        "WHERE q.token_value=?";

    private static final String SQL_FIND_ACTIVE_BY_LECTURE =
        "SELECT q.* FROM qr_token q " +
        "WHERE q.lecture_id=? AND q.is_expired=FALSE AND q.expires_at > NOW() " +
        "ORDER BY q.generated_at DESC LIMIT 1";

    private static final String SQL_EXPIRE_OLD =
        "UPDATE qr_token SET is_expired=TRUE WHERE expires_at < NOW() AND is_expired=FALSE";

    private static final String SQL_EXPIRE_BY_LECTURE =
        "UPDATE qr_token SET is_expired=TRUE WHERE lecture_id=? AND is_expired=FALSE";

    private static final String SQL_INCREMENT_SCAN =
        "UPDATE qr_token SET scan_count=scan_count+1 WHERE token_id=?";

    // ---- CRUD --------------------------------------------------------

    @Override
    public int insert(QRToken token) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt      (1, token.getLectureId());
            ps.setString   (2, token.getTokenValue());
            ps.setString   (3, token.getQrData());
            ps.setTimestamp(4, token.getExpiresAt());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    token.setTokenId(id);
                    log.info("QR token created: id={}, lecture={}", id, token.getLectureId());
                    return id;
                }
            }
        } finally {
            db.releaseConnection(conn);
        }
        return -1;
    }

    @Override
    public int update(QRToken t) throws SQLException { return 0; }

    @Override
    public int delete(Integer id) throws SQLException { return 0; }

    @Override
    public Optional<QRToken> findById(Integer id) throws SQLException {
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

    public Optional<QRToken> findByTokenValue(String tokenValue) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_TOKEN)) {
            ps.setString(1, tokenValue);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } finally {
            db.releaseConnection(conn);
        }
        return Optional.empty();
    }

    public Optional<QRToken> findActiveTokenForLecture(int lectureId) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_ACTIVE_BY_LECTURE)) {
            ps.setInt(1, lectureId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } finally {
            db.releaseConnection(conn);
        }
        return Optional.empty();
    }

    @Override
    public List<QRToken> findAll() throws SQLException { return new ArrayList<>(); }

    public void expireOldTokens() throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_EXPIRE_OLD)) {
            int rows = ps.executeUpdate();
            if (rows > 0) log.info("Expired {} stale QR tokens.", rows);
        } finally {
            db.releaseConnection(conn);
        }
    }

    public void expireTokensForLecture(int lectureId) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_EXPIRE_BY_LECTURE)) {
            ps.setInt(1, lectureId);
            ps.executeUpdate();
        } finally {
            db.releaseConnection(conn);
        }
    }

    public void incrementScanCount(int tokenId) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_INCREMENT_SCAN)) {
            ps.setInt(1, tokenId);
            ps.executeUpdate();
        } finally {
            db.releaseConnection(conn);
        }
    }

    // ---- Mapper ------------------------------------------------------

    private QRToken map(ResultSet rs) throws SQLException {
        QRToken q = new QRToken();
        q.setTokenId    (rs.getInt      ("token_id"));
        q.setLectureId  (rs.getInt      ("lecture_id"));
        q.setTokenValue (rs.getString   ("token_value"));
        q.setQrData     (rs.getString   ("qr_data"));
        q.setGeneratedAt(rs.getTimestamp("generated_at"));
        q.setExpiresAt  (rs.getTimestamp("expires_at"));
        q.setUsed       (rs.getBoolean  ("is_used"));
        q.setExpired    (rs.getBoolean  ("is_expired"));
        q.setScanCount  (rs.getInt      ("scan_count"));
        try {
            q.setSubjectId  (rs.getInt   ("subject_id"));
            q.setTeacherId  (rs.getInt   ("teacher_id"));
            q.setClassId    (rs.getInt   ("class_id"));
            q.setSubjectName(rs.getString("subject_name"));
            q.setTeacherName(rs.getString("teacher_name"));
        } catch (SQLException ignored) {}
        return q;
    }
}
