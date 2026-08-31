package com.attendance.model;

import java.sql.Timestamp;

/** Model: Attendance record */
public class Attendance {

    public enum MarkedBy { QR_SCAN, MANUAL, SYSTEM }
    public enum Status   { PRESENT, ABSENT, LATE, EXCUSED }

    private int       attendanceId;
    private int       studentId;
    private String    studentName;
    private String    enrollmentNo;
    private int       lectureId;
    private int       tokenId;
    private Timestamp markedAt;
    private MarkedBy  markedBy;
    private Status    status;
    private String    remarks;
    private String    deviceInfo;

    // Joined fields
    private String    subjectName;
    private String    subjectCode;
    private String    className;
    private String    lectureDate;
    private String    startTime;
    private String    teacherName;
    private String    departmentName;

    public Attendance() {
        this.markedBy = MarkedBy.QR_SCAN;
        this.status   = Status.PRESENT;
    }

    // ---- Getters & Setters -------------------------------------------

    public int getAttendanceId()              { return attendanceId; }
    public void setAttendanceId(int v)        { attendanceId = v; }

    public int getStudentId()                 { return studentId; }
    public void setStudentId(int v)           { studentId = v; }

    public String getStudentName()            { return studentName; }
    public void setStudentName(String v)      { studentName = v; }

    public String getEnrollmentNo()           { return enrollmentNo; }
    public void setEnrollmentNo(String v)     { enrollmentNo = v; }

    public int getLectureId()                 { return lectureId; }
    public void setLectureId(int v)           { lectureId = v; }

    public int getTokenId()                   { return tokenId; }
    public void setTokenId(int v)             { tokenId = v; }

    public Timestamp getMarkedAt()            { return markedAt; }
    public void setMarkedAt(Timestamp v)      { markedAt = v; }

    public MarkedBy getMarkedBy()             { return markedBy; }
    public void setMarkedBy(MarkedBy v)       { markedBy = v; }

    public Status getStatus()                 { return status; }
    public void setStatus(Status v)           { status = v; }

    public String getRemarks()                { return remarks; }
    public void setRemarks(String v)          { remarks = v; }

    public String getDeviceInfo()             { return deviceInfo; }
    public void setDeviceInfo(String v)       { deviceInfo = v; }

    public String getSubjectName()            { return subjectName; }
    public void setSubjectName(String v)      { subjectName = v; }

    public String getSubjectCode()            { return subjectCode; }
    public void setSubjectCode(String v)      { subjectCode = v; }

    public String getClassName()              { return className; }
    public void setClassName(String v)        { className = v; }

    public String getLectureDate()            { return lectureDate; }
    public void setLectureDate(String v)      { lectureDate = v; }

    public String getStartTime()              { return startTime; }
    public void setStartTime(String v)        { startTime = v; }

    public String getTeacherName()            { return teacherName; }
    public void setTeacherName(String v)      { teacherName = v; }

    public String getDepartmentName()         { return departmentName; }
    public void setDepartmentName(String v)   { departmentName = v; }

    @Override
    public String toString() {
        return "Attendance{id=" + attendanceId + ", student=" + studentId
                + ", lecture=" + lectureId + ", status=" + status + "}";
    }
}
