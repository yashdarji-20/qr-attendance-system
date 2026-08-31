package com.attendance.model;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * Model: Student
 */
public class Student {

    private int       studentId;
    private String    enrollmentNo;
    private String    firstName;
    private String    lastName;
    private String    email;
    private String    phone;
    private String    passwordHash;
    private int       departmentId;
    private String    departmentName;
    private int       semester;
    private int       yearOfStudy;
    private Date      dateOfBirth;
    private String    address;
    private byte[]    profileImage;
    private boolean   isActive;
    private Timestamp lastLogin;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Computed / joined fields
    private double    attendancePercentage;
    private int       totalLectures;
    private int       attendedLectures;

    public Student() {}

    public Student(String enrollmentNo, String firstName, String lastName,
                   String email, String phone, String passwordHash,
                   int departmentId, int semester, int yearOfStudy) {
        this.enrollmentNo = enrollmentNo;
        this.firstName    = firstName;
        this.lastName     = lastName;
        this.email        = email;
        this.phone        = phone;
        this.passwordHash = passwordHash;
        this.departmentId = departmentId;
        this.semester     = semester;
        this.yearOfStudy  = yearOfStudy;
        this.isActive     = true;
    }

    public String getFullName() { return firstName + " " + lastName; }

    // ---- Getters & Setters -------------------------------------------

    public int getStudentId()                { return studentId; }
    public void setStudentId(int v)          { studentId = v; }

    public String getEnrollmentNo()          { return enrollmentNo; }
    public void setEnrollmentNo(String v)    { enrollmentNo = v; }

    public String getFirstName()             { return firstName; }
    public void setFirstName(String v)       { firstName = v; }

    public String getLastName()              { return lastName; }
    public void setLastName(String v)        { lastName = v; }

    public String getEmail()                 { return email; }
    public void setEmail(String v)           { email = v; }

    public String getPhone()                 { return phone; }
    public void setPhone(String v)           { phone = v; }

    public String getPasswordHash()          { return passwordHash; }
    public void setPasswordHash(String v)    { passwordHash = v; }

    public int getDepartmentId()             { return departmentId; }
    public void setDepartmentId(int v)       { departmentId = v; }

    public String getDepartmentName()        { return departmentName; }
    public void setDepartmentName(String v)  { departmentName = v; }

    public int getSemester()                 { return semester; }
    public void setSemester(int v)           { semester = v; }

    public int getYearOfStudy()              { return yearOfStudy; }
    public void setYearOfStudy(int v)        { yearOfStudy = v; }

    public Date getDateOfBirth()             { return dateOfBirth; }
    public void setDateOfBirth(Date v)       { dateOfBirth = v; }

    public String getAddress()               { return address; }
    public void setAddress(String v)         { address = v; }

    public byte[] getProfileImage()          { return profileImage; }
    public void setProfileImage(byte[] v)    { profileImage = v; }

    public boolean isActive()                { return isActive; }
    public void setActive(boolean v)         { isActive = v; }

    public Timestamp getLastLogin()          { return lastLogin; }
    public void setLastLogin(Timestamp v)    { lastLogin = v; }

    public Timestamp getCreatedAt()          { return createdAt; }
    public void setCreatedAt(Timestamp v)    { createdAt = v; }

    public Timestamp getUpdatedAt()          { return updatedAt; }
    public void setUpdatedAt(Timestamp v)    { updatedAt = v; }

    public double getAttendancePercentage()       { return attendancePercentage; }
    public void setAttendancePercentage(double v) { attendancePercentage = v; }

    public int getTotalLectures()            { return totalLectures; }
    public void setTotalLectures(int v)      { totalLectures = v; }

    public int getAttendedLectures()         { return attendedLectures; }
    public void setAttendedLectures(int v)   { attendedLectures = v; }

    @Override
    public String toString() {
        return "Student{id=" + studentId + ", enroll='" + enrollmentNo
                + "', name='" + getFullName() + "'}";
    }
}
