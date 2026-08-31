package com.attendance.service;

import com.attendance.dao.AttendanceDAO;
import com.attendance.dao.QRTokenDAO;
import com.attendance.dao.StudentDAO;
import com.attendance.model.Attendance;
import com.attendance.model.QRToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Service orchestrating attendance marking, validation, and reporting.
 */
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceDAO attendanceDAO;
    private final QRCodeService qrCodeService;
    private final QRTokenDAO    qrTokenDAO;
    private final StudentDAO    studentDAO;

    public AttendanceService() {
        this.attendanceDAO = new AttendanceDAO();
        this.qrCodeService = new QRCodeService();
        this.qrTokenDAO    = new QRTokenDAO();
        this.studentDAO    = new StudentDAO();
    }

    // ---- Mark Attendance via QR -------------------------------------

    /**
     * Mark attendance for a student using a scanned QR payload string.
     *
     * @param studentId    the logged-in student
     * @param qrPayload    the raw string decoded from the QR image
     * @return MarkResult with success/failure and message
     */
    public MarkResult markAttendanceByQR(int studentId, String qrPayload) {
        try {
            // 1. Extract token from payload
            String tokenValue = qrCodeService.extractToken(qrPayload);
            if (tokenValue == null || tokenValue.isBlank()) {
                return new MarkResult(false, "Invalid QR code format.");
            }

            // 2. Validate token against DB
            QRCodeService.ValidationResult vr = qrCodeService.validateToken(tokenValue);
            if (!vr.isValid()) {
                return new MarkResult(false, vr.getMessage());
            }

            QRToken token = vr.getToken();

            // 3. Check for duplicate attendance
            if (attendanceDAO.exists(studentId, token.getLectureId())) {
                return new MarkResult(false, "Attendance already recorded for this lecture.");
            }

            // 4. Insert attendance record
            Attendance att = new Attendance();
            att.setStudentId(studentId);
            att.setLectureId(token.getLectureId());
            att.setTokenId  (token.getTokenId());
            att.setMarkedBy (Attendance.MarkedBy.QR_SCAN);
            att.setStatus   (Attendance.Status.PRESENT);

            int id = attendanceDAO.insert(att);
            if (id < 0) {
                return new MarkResult(false, "Failed to record attendance. Please try again.");
            }

            // 5. Increment scan counter
            qrTokenDAO.incrementScanCount(token.getTokenId());

            log.info("Attendance marked: student={}, lecture={}, token={}",
                    studentId, token.getLectureId(), tokenValue);
            return new MarkResult(true, "Attendance marked successfully!");

        } catch (Exception e) {
            log.error("Error marking attendance: {}", e.getMessage());
            return new MarkResult(false, "An error occurred: " + e.getMessage());
        }
    }

    /**
     * Manual attendance marking by teacher (no QR).
     */
    public MarkResult markManually(int studentId, int lectureId,
                                   Attendance.Status status, String remarks) {
        try {
            if (attendanceDAO.exists(studentId, lectureId)) {
                return new MarkResult(false, "Attendance already recorded for this student.");
            }
            Attendance att = new Attendance();
            att.setStudentId(studentId);
            att.setLectureId(lectureId);
            att.setMarkedBy (Attendance.MarkedBy.MANUAL);
            att.setStatus   (status);
            att.setRemarks  (remarks);
            attendanceDAO.insert(att);
            return new MarkResult(true, "Attendance marked manually.");
        } catch (SQLException e) {
            log.error("Manual attendance error: {}", e.getMessage());
            return new MarkResult(false, "Error: " + e.getMessage());
        }
    }

    // ---- Queries -----------------------------------------------------

    public List<Attendance> getStudentAttendance(int studentId) throws SQLException {
        return attendanceDAO.findByStudent(studentId);
    }

    public List<Attendance> getLectureAttendance(int lectureId) throws SQLException {
        return attendanceDAO.findByLecture(lectureId);
    }

    public List<Attendance> getAttendanceByDateRange(int classId,
                                                      LocalDate from,
                                                      LocalDate to) throws SQLException {
        return attendanceDAO.findByDateRange(
                classId,
                Date.valueOf(from),
                Date.valueOf(to)
        );
    }

    public List<Object[]> getAttendancePercentage(int subjectId, int classId) throws SQLException {
        return attendanceDAO.getAttendancePercentage(subjectId, classId);
    }

    /**
     * Compute overall attendance percentage for a student.
     * Returns value 0–100.
     */
    public double computeStudentOverallPercentage(int studentId) throws SQLException {
        List<Attendance> records = attendanceDAO.findByStudent(studentId);
        if (records.isEmpty()) return 0.0;

        long present = records.stream()
                .filter(a -> a.getStatus() == Attendance.Status.PRESENT
                          || a.getStatus() == Attendance.Status.LATE)
                .count();
        return (present * 100.0) / records.size();
    }

    // ---- Nested Result Class ----------------------------------------

    public static final class MarkResult {
        private final boolean success;
        private final String  message;

        public MarkResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String  getMessage(){ return message; }
    }
}
