# QR Code Based Attendance Management System
### Final Year College Project — Java Swing + MySQL + ZXing

---

## 📋 Overview

A professional desktop application that enables teachers to generate QR codes for lectures,
and students to scan them to automatically mark attendance.  
Built with Java 17, Swing UI, MySQL, and modern libraries.

---

## 🏗 Architecture

```
MVC Pattern
├── Model       — POJOs: Teacher, Student, Lecture, QRToken, Attendance
├── View        — Swing panels: LoginView, TeacherDashboard, StudentDashboard
├── Controller  — Service layer: AuthService, QRCodeService, AttendanceService
├── DAO         — JDBC data access: TeacherDAO, StudentDAO, AttendanceDAO, ...
└── Reports     — PDF (iText) and Excel (Apache POI) generators
```

---

## 🛠 Tech Stack

| Component         | Technology            |
|-------------------|-----------------------|
| Language          | Java 17               |
| GUI Framework     | Java Swing + FlatLaf  |
| Database          | MySQL 8.0             |
| DB Connectivity   | JDBC                  |
| QR Code           | Google ZXing 3.5.2    |
| PDF Export        | iText PDF 7.2.5       |
| Excel Export      | Apache POI 5.2.4      |
| Charts            | JFreeChart 1.5.4      |
| Password Security | BCrypt                |
| Build Tool        | Maven                 |

---

## ⚙️ Prerequisites

- Java 17 (JDK)
- MySQL 8.0+
- Maven 3.8+
- IDE: IntelliJ IDEA or NetBeans

---

## 🚀 Installation Guide

### Step 1 — Database Setup

```bash
# Log in to MySQL
mysql -u root -p

# Run the schema file
source /path/to/attendance-system/database/schema.sql
```

This creates the database, all tables, views, stored procedures, and seeds default data.

### Step 2 — Configure Database Connection

Edit `resources/db.properties`:

```properties
db.host=localhost
db.port=3306
db.name=attendance_system
db.username=root
db.password=yourpassword
```

### Step 3 — Build with Maven

```bash
cd attendance-system
mvn clean package
```

This produces `target/AttendanceSystem.jar`.

### Step 4 — Run

```bash
java -jar target/AttendanceSystem.jar
```

Or via IDE: Run `com.attendance.Main`

---

## 🔑 Default Credentials

### Teacher Login
| Field       | Value          |
|-------------|----------------|
| Employee ID | EMP001         |
| Password    | Teacher@123    |

### Student Login
| Field         | Value        |
|---------------|--------------|
| Enrollment No | 2021CSE001   |
| Password      | Student@123  |

---

## 📦 Project Structure

```
attendance-system/
├── database/
│   └── schema.sql                    ← Complete MySQL schema + seed data
├── src/com/attendance/
│   ├── Main.java                     ← Application entry point
│   ├── database/
│   │   ├── DatabaseConfig.java
│   │   └── DatabaseConnection.java   ← JDBC connection pool
│   ├── model/
│   │   ├── Teacher.java
│   │   ├── Student.java
│   │   ├── Department.java
│   │   ├── Subject.java
│   │   ├── ClassRoom.java
│   │   ├── Lecture.java
│   │   ├── QRToken.java
│   │   └── Attendance.java
│   ├── dao/
│   │   ├── GenericDAO.java           ← Interface
│   │   ├── TeacherDAO.java
│   │   ├── StudentDAO.java
│   │   ├── AttendanceDAO.java
│   │   ├── LectureDAO.java
│   │   ├── QRTokenDAO.java
│   │   ├── SubjectDAO.java
│   │   ├── ClassRoomDAO.java
│   │   └── DepartmentDAO.java
│   ├── service/
│   │   ├── AuthService.java          ← Login / BCrypt
│   │   ├── QRCodeService.java        ← QR generation + validation
│   │   └── AttendanceService.java    ← Attendance logic
│   ├── view/
│   │   ├── LoginView.java
│   │   ├── TeacherDashboardView.java
│   │   ├── TeacherHomePanel.java
│   │   ├── ManageStudentsPanel.java
│   │   ├── GenerateQRPanel.java
│   │   ├── ViewAttendancePanel.java
│   │   ├── ReportsPanel.java
│   │   └── StudentDashboardView.java
│   ├── reports/
│   │   ├── PDFReportGenerator.java
│   │   └── ExcelReportGenerator.java
│   └── utils/
│       └── UITheme.java              ← Colours, fonts, component factories
├── resources/
│   ├── db.properties
│   └── logback.xml
└── pom.xml
```

---

## 🖥 Feature List

### Teacher Module
- Login / Logout with BCrypt password verification
- Dashboard with stat cards (total students, today's lectures, etc.)
- Manage Students: Add / Edit / Delete / Search
- Generate QR Code with configurable expiry (30s default)
- Live countdown timer on QR display
- View Attendance by class / date range
- Attendance Reports with charts (bar + pie via JFreeChart)
- Export PDF (iText) and Excel (Apache POI)

### Student Module
- Login / Logout
- Dashboard with overall attendance percentage
- Scan QR Code (select image file → auto-decode via ZXing)
- Manual token input fallback
- Attendance History (colour-coded by status)
- Profile view

### QR Code Logic
- 30-second expiry window (configurable)
- One QR per lecture
- Duplicate scan prevention (one attendance per lecture per student)
- Token stored in DB with expiry timestamp
- Colour-coded countdown: green → yellow → red

---

## 🔒 Security

- All passwords hashed with BCrypt (cost factor 12)
- QR tokens are UUID-based (32-char alphanumeric)
- Session management via static session holders
- SQL injection prevention via PreparedStatement

---

## 📊 Database Tables

| Table               | Description                              |
|---------------------|------------------------------------------|
| department          | Academic departments                     |
| teacher             | Teaching staff                           |
| student             | Student records                          |
| class               | Class sections (e.g. CSE-6A)             |
| student_class       | Student ↔ Class enrollment               |
| subject             | Academic subjects                        |
| teacher_subject     | Teacher ↔ Subject ↔ Class assignment     |
| lecture             | Individual lecture sessions              |
| qr_token            | Generated QR tokens (30s TTL)            |
| attendance          | Attendance records                       |
| attendance_summary  | Materialized summary for performance     |
| system_config       | App configuration key/value store        |
| audit_log           | Action audit trail                       |

---

## 🤝 Credits

Final Year Project — Computer Science Engineering  
Tech Stack: Java 17, Swing, MySQL, ZXing, iText, Apache POI, JFreeChart, BCrypt, FlatLaf
