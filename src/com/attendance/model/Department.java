package com.attendance.model;

import java.sql.Timestamp;

/** Model: Department */
public class Department {
    private int       departmentId;
    private String    departmentName;
    private String    departmentCode;
    private String    description;
    private boolean   isActive;
    private Timestamp createdAt;

    public Department() {}

    public Department(String name, String code, String description) {
        this.departmentName = name;
        this.departmentCode = code;
        this.description    = description;
        this.isActive       = true;
    }

    public int getDepartmentId()             { return departmentId; }
    public void setDepartmentId(int v)       { departmentId = v; }

    public String getDepartmentName()        { return departmentName; }
    public void setDepartmentName(String v)  { departmentName = v; }

    public String getDepartmentCode()        { return departmentCode; }
    public void setDepartmentCode(String v)  { departmentCode = v; }

    public String getDescription()           { return description; }
    public void setDescription(String v)     { description = v; }

    public boolean isActive()                { return isActive; }
    public void setActive(boolean v)         { isActive = v; }

    public Timestamp getCreatedAt()          { return createdAt; }
    public void setCreatedAt(Timestamp v)    { createdAt = v; }

    @Override
    public String toString() { return departmentName + " (" + departmentCode + ")"; }
}
