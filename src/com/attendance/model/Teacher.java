package com.attendance.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Model: Teacher
 * Represents a teaching staff member.
 */
public class Teacher {

    private int       teacherId;
    private String    employeeId;
    private String    firstName;
    private String    lastName;
    private String    email;
    private String    phone;
    private String    passwordHash;
    private int       departmentId;
    private String    departmentName;   // joined field
    private String    designation;
    private byte[]    profileImage;
    private boolean   isActive;
    private Timestamp lastLogin;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // ---- Constructors ------------------------------------------------

    public Teacher() {}

    public Teacher(String employeeId, String firstName, String lastName,
                   String email, String phone, String passwordHash,
                   int departmentId, String designation) {
        this.employeeId   = employeeId;
        this.firstName    = firstName;
        this.lastName     = lastName;
        this.email        = email;
        this.phone        = phone;
        this.passwordHash = passwordHash;
        this.departmentId = departmentId;
        this.designation  = designation;
        this.isActive     = true;
    }

    // ---- Business helpers --------------------------------------------

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isLoggedIn() {
        return lastLogin != null;
    }

    // ---- Getters & Setters -------------------------------------------

    public int getTeacherId()              { return teacherId; }
    public void setTeacherId(int v)        { teacherId = v; }

    public String getEmployeeId()          { return employeeId; }
    public void setEmployeeId(String v)    { employeeId = v; }

    public String getFirstName()           { return firstName; }
    public void setFirstName(String v)     { firstName = v; }

    public String getLastName()            { return lastName; }
    public void setLastName(String v)      { lastName = v; }

    public String getEmail()               { return email; }
    public void setEmail(String v)         { email = v; }

    public String getPhone()               { return phone; }
    public void setPhone(String v)         { phone = v; }

    public String getPasswordHash()        { return passwordHash; }
    public void setPasswordHash(String v)  { passwordHash = v; }

    public int getDepartmentId()           { return departmentId; }
    public void setDepartmentId(int v)     { departmentId = v; }

    public String getDepartmentName()      { return departmentName; }
    public void setDepartmentName(String v){ departmentName = v; }

    public String getDesignation()         { return designation; }
    public void setDesignation(String v)   { designation = v; }

    public byte[] getProfileImage()        { return profileImage; }
    public void setProfileImage(byte[] v)  { profileImage = v; }

    public boolean isActive()              { return isActive; }
    public void setActive(boolean v)       { isActive = v; }

    public Timestamp getLastLogin()        { return lastLogin; }
    public void setLastLogin(Timestamp v)  { lastLogin = v; }

    public Timestamp getCreatedAt()        { return createdAt; }
    public void setCreatedAt(Timestamp v)  { createdAt = v; }

    public Timestamp getUpdatedAt()        { return updatedAt; }
    public void setUpdatedAt(Timestamp v)  { updatedAt = v; }

    @Override
    public String toString() {
        return "Teacher{id=" + teacherId + ", empId='" + employeeId
                + "', name='" + getFullName() + "'}";
    }
}
