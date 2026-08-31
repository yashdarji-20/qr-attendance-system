package com.attendance.model;

import java.sql.Timestamp;

/** Model: Class (renamed to ClassRoom to avoid keyword clash) */
public class ClassRoom {
    private int       classId;
    private String    className;
    private String    classCode;
    private int       departmentId;
    private String    departmentName;
    private int       semester;
    private int       yearOfStudy;
    private String    roomNo;
    private int       capacity;
    private boolean   isActive;
    private Timestamp createdAt;

    public ClassRoom() {}

    public ClassRoom(String name, String code, int departmentId,
                     int semester, int yearOfStudy, String roomNo, int capacity) {
        this.className    = name;
        this.classCode    = code;
        this.departmentId = departmentId;
        this.semester     = semester;
        this.yearOfStudy  = yearOfStudy;
        this.roomNo       = roomNo;
        this.capacity     = capacity;
        this.isActive     = true;
    }

    public int getClassId()                  { return classId; }
    public void setClassId(int v)            { classId = v; }

    public String getClassName()             { return className; }
    public void setClassName(String v)       { className = v; }

    public String getClassCode()             { return classCode; }
    public void setClassCode(String v)       { classCode = v; }

    public int getDepartmentId()             { return departmentId; }
    public void setDepartmentId(int v)       { departmentId = v; }

    public String getDepartmentName()        { return departmentName; }
    public void setDepartmentName(String v)  { departmentName = v; }

    public int getSemester()                 { return semester; }
    public void setSemester(int v)           { semester = v; }

    public int getYearOfStudy()              { return yearOfStudy; }
    public void setYearOfStudy(int v)        { yearOfStudy = v; }

    public String getRoomNo()                { return roomNo; }
    public void setRoomNo(String v)          { roomNo = v; }

    public int getCapacity()                 { return capacity; }
    public void setCapacity(int v)           { capacity = v; }

    public boolean isActive()                { return isActive; }
    public void setActive(boolean v)         { isActive = v; }

    public Timestamp getCreatedAt()          { return createdAt; }
    public void setCreatedAt(Timestamp v)    { createdAt = v; }

    @Override
    public String toString() { return className + " (" + classCode + ")"; }
}
