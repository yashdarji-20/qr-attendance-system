package com.attendance.model;

import java.sql.Timestamp;
import java.time.Instant;

/** Model: QR Token — one token per lecture, valid for 30 seconds */
public class QRToken {

    private int       tokenId;
    private int       lectureId;
    private String    tokenValue;   // unique UUID
    private String    qrData;       // JSON payload encoded in QR
    private Timestamp generatedAt;
    private Timestamp expiresAt;
    private boolean   isUsed;
    private boolean   isExpired;
    private int       scanCount;

    // Joined / transient
    private String    subjectName;
    private String    teacherName;
    private int       teacherId;
    private int       subjectId;
    private int       classId;

    public QRToken() {}

    // ---- Business logic ----------------------------------------------

    /** Returns true if the token is still within its validity window. */
    public boolean isValid() {
        if (isExpired || isUsed) return false;
        return expiresAt != null && Instant.now().isBefore(expiresAt.toInstant());
    }

    /** Seconds remaining before expiry (negative = already expired). */
    public long secondsRemaining() {
        if (expiresAt == null) return -1;
        return (expiresAt.toInstant().toEpochMilli() - Instant.now().toEpochMilli()) / 1000;
    }

    // ---- Getters & Setters -------------------------------------------

    public int getTokenId()                 { return tokenId; }
    public void setTokenId(int v)           { tokenId = v; }

    public int getLectureId()               { return lectureId; }
    public void setLectureId(int v)         { lectureId = v; }

    public String getTokenValue()           { return tokenValue; }
    public void setTokenValue(String v)     { tokenValue = v; }

    public String getQrData()               { return qrData; }
    public void setQrData(String v)         { qrData = v; }

    public Timestamp getGeneratedAt()       { return generatedAt; }
    public void setGeneratedAt(Timestamp v) { generatedAt = v; }

    public Timestamp getExpiresAt()         { return expiresAt; }
    public void setExpiresAt(Timestamp v)   { expiresAt = v; }

    public boolean isUsed()                 { return isUsed; }
    public void setUsed(boolean v)          { isUsed = v; }

    public boolean isExpired()              { return isExpired; }
    public void setExpired(boolean v)       { isExpired = v; }

    public int getScanCount()               { return scanCount; }
    public void setScanCount(int v)         { scanCount = v; }

    public String getSubjectName()          { return subjectName; }
    public void setSubjectName(String v)    { subjectName = v; }

    public String getTeacherName()          { return teacherName; }
    public void setTeacherName(String v)    { teacherName = v; }

    public int getTeacherId()               { return teacherId; }
    public void setTeacherId(int v)         { teacherId = v; }

    public int getSubjectId()               { return subjectId; }
    public void setSubjectId(int v)         { subjectId = v; }

    public int getClassId()                 { return classId; }
    public void setClassId(int v)           { classId = v; }
}
