package com.attendance.model;

import java.sql.Timestamp;

/** Model: Subject */
public class Subject {
    private int       subjectId;
    private String    subjectName;
    private String    subjectCode;
    private int       departmentId;
    private String    departmentName;
    private int       credits;
    private int       semester;
    private String    description;
    private boolean   isActive;
    private Timestamp createdAt;

    public Subject() {}

    public Subject(String name, String code, int departmentId, int credits, int semester) {
        this.subjectName  = name;
        this.subjectCode  = code;
        this.departmentId = departmentId;
        this.credits      = credits;
        this.semester     = semester;
        this.isActive     = true;
    }

    public int getSubjectId()                { return subjectId; }
    public void setSubjectId(int v)          { subjectId = v; }

    public String getSubjectName()           { return subjectName; }
    public void setSubjectName(String v)     { subjectName = v; }

    public String getSubjectCode()           { return subjectCode; }
    public void setSubjectCode(String v)     { subjectCode = v; }

    public int getDepartmentId()             { return departmentId; }
    public void setDepartmentId(int v)       { departmentId = v; }

    public String getDepartmentName()        { return departmentName; }
    public void setDepartmentName(String v)  { departmentName = v; }

    public int getCredits()                  { return credits; }
    public void setCredits(int v)            { credits = v; }

    public int getSemester()                 { return semester; }
    public void setSemester(int v)           { semester = v; }

    public String getDescription()           { return description; }
    public void setDescription(String v)     { description = v; }

    public boolean isActive()                { return isActive; }
    public void setActive(boolean v)         { isActive = v; }

    public Timestamp getCreatedAt()          { return createdAt; }
    public void setCreatedAt(Timestamp v)    { createdAt = v; }

    @Override
    public String toString() { return subjectName + " [" + subjectCode + "]"; }
}
