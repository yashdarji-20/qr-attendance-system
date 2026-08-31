package com.attendance.model;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

/** Model: Lecture */
public class Lecture {

    public enum LectureType   { REGULAR, EXTRA, LAB, TUTORIAL }
    public enum LectureStatus { SCHEDULED, ONGOING, COMPLETED, CANCELLED }

    private int           lectureId;
    private int           subjectId;
    private String        subjectName;
    private String        subjectCode;
    private int           teacherId;
    private String        teacherName;
    private int           classId;
    private String        className;
    private Date          lectureDate;
    private Time          startTime;
    private Time          endTime;
    private String        topic;
    private LectureType   lectureType;
    private LectureStatus status;
    private String        notes;
    private Timestamp     createdAt;
    private int           presentCount;   // computed
    private int           totalStudents;  // computed

    public Lecture() {
        this.lectureType = LectureType.REGULAR;
        this.status      = LectureStatus.SCHEDULED;
    }

    // ---- Getters & Setters -------------------------------------------

    public int getLectureId()                    { return lectureId; }
    public void setLectureId(int v)              { lectureId = v; }

    public int getSubjectId()                    { return subjectId; }
    public void setSubjectId(int v)              { subjectId = v; }

    public String getSubjectName()               { return subjectName; }
    public void setSubjectName(String v)         { subjectName = v; }

    public String getSubjectCode()               { return subjectCode; }
    public void setSubjectCode(String v)         { subjectCode = v; }

    public int getTeacherId()                    { return teacherId; }
    public void setTeacherId(int v)              { teacherId = v; }

    public String getTeacherName()               { return teacherName; }
    public void setTeacherName(String v)         { teacherName = v; }

    public int getClassId()                      { return classId; }
    public void setClassId(int v)                { classId = v; }

    public String getClassName()                 { return className; }
    public void setClassName(String v)           { className = v; }

    public Date getLectureDate()                 { return lectureDate; }
    public void setLectureDate(Date v)           { lectureDate = v; }

    public Time getStartTime()                   { return startTime; }
    public void setStartTime(Time v)             { startTime = v; }

    public Time getEndTime()                     { return endTime; }
    public void setEndTime(Time v)               { endTime = v; }

    public String getTopic()                     { return topic; }
    public void setTopic(String v)               { topic = v; }

    public LectureType getLectureType()          { return lectureType; }
    public void setLectureType(LectureType v)    { lectureType = v; }

    public LectureStatus getStatus()             { return status; }
    public void setStatus(LectureStatus v)       { status = v; }

    public String getNotes()                     { return notes; }
    public void setNotes(String v)               { notes = v; }

    public Timestamp getCreatedAt()              { return createdAt; }
    public void setCreatedAt(Timestamp v)        { createdAt = v; }

    public int getPresentCount()                 { return presentCount; }
    public void setPresentCount(int v)           { presentCount = v; }

    public int getTotalStudents()                { return totalStudents; }
    public void setTotalStudents(int v)          { totalStudents = v; }

    @Override
    public String toString() {
        return "Lecture{id=" + lectureId + ", subject='" + subjectName
                + "', date=" + lectureDate + "}";
    }
}
