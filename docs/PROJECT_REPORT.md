# PROJECT REPORT

## QR Code Based Attendance Management System

**Final Year B.Tech Project — Computer Science Engineering**  
**Academic Year: 2024–2025**

---

## Abstract

The QR Code Based Attendance Management System is a comprehensive desktop application developed using Java 17 with Swing GUI, MySQL database, and industry-standard libraries including ZXing (QR generation), iText PDF, Apache POI (Excel), and JFreeChart. The system automates the traditional manual attendance process by enabling teachers to generate time-limited QR codes for each lecture, which students scan to mark their presence automatically. The application follows the MVC architectural pattern, provides role-based access (Teacher/Student), enforces duplicate-prevention and QR-expiry rules, and produces detailed PDF/Excel attendance reports with analytical charts.

---

## 1. Introduction

Attendance tracking is a mandatory process in all educational institutions. Traditional paper-based methods are time-consuming, error-prone, and difficult to aggregate for reporting. With increasing smartphone adoption and digital infrastructure, QR code-based systems offer a fast, accurate, and tamper-resistant alternative.

This project delivers a fully functional, production-quality desktop application that:
- Automates attendance marking via time-limited QR codes
- Maintains a normalised relational database
- Provides analytical dashboards and exportable reports
- Enforces strict business rules (no duplicates, 30s QR expiry)

---

## 2. Problem Statement

Educational institutions face the following challenges with manual attendance:
1. **Time waste**: Roll-call in large classes wastes 5–10 minutes per lecture
2. **Proxy attendance**: Students mark attendance on behalf of absent peers
3. **Record accuracy**: Paper registers are lost, damaged, or tampered with
4. **Reporting effort**: Aggregating monthly/subject-wise reports takes hours
5. **Transparency**: Students often don't know their attendance standing

---

## 3. Objectives

- Design and implement a QR-code-based attendance system for colleges
- Generate per-lecture QR codes that expire after 30 seconds (anti-proxy)
- Support two user roles: Teacher and Student
- Maintain a normalised MySQL database with full referential integrity
- Produce PDF and Excel reports with attendance percentages
- Display attendance trends via charts (bar and pie)
- Follow MVC architecture with clean separation of concerns

---

## 4. Scope

**In Scope:**
- Teacher and Student login with BCrypt password security
- QR code generation and decoding (ZXing library)
- Attendance marking with 30s expiry and duplicate prevention
- Student management (CRUD operations)
- Attendance reports: daily, weekly, monthly, subject-wise
- PDF and Excel export
- Attendance percentage calculations

**Out of Scope:**
- Mobile application (this is a desktop app)
- Real-time camera scanning (uses image file input)
- Email notifications (architecture ready, not implemented)
- Multi-institution multi-tenancy

---

## 5. Literature Survey

| Reference | Technology/Approach | Limitation addressed by our system |
|-----------|--------------------|------------------------------------|
| RFID-based systems | Hardware cards | No hardware dependency |
| Biometric systems | Fingerprint/Face | Privacy concerns eliminated |
| Simple QR systems | No expiry | 30-second expiry prevents proxy |
| Spreadsheet-based | Manual entry | Fully automated via QR scan |

QR codes were chosen for:
- Zero hardware cost (students use existing devices or screenshots)
- Instant generation and decoding
- Error-correction built into QR standard (up to 30% data recovery)
- Open-source library availability (ZXing)

---

## 6. Methodology

### 6.1 Software Development Lifecycle
We followed the **Iterative SDLC** model:
1. Requirements analysis
2. Database design (normalisation to 3NF)
3. Backend (DAO/Service) implementation
4. UI development (Swing panels)
5. Integration and testing
6. Documentation

### 6.2 Architecture — MVC
```
┌─────────────────────────────────────────────────┐
│                   VIEW (Swing)                  │
│  LoginView │ TeacherDashboard │ StudentDashboard│
└────────────────────┬────────────────────────────┘
                     │ calls
┌────────────────────▼────────────────────────────┐
│               SERVICE LAYER                     │
│  AuthService │ QRCodeService │ AttendanceService│
└────────────────────┬────────────────────────────┘
                     │ calls
┌────────────────────▼────────────────────────────┐
│                  DAO LAYER                      │
│  TeacherDAO │ StudentDAO │ AttendanceDAO │ ...  │
└────────────────────┬────────────────────────────┘
                     │ JDBC
┌────────────────────▼────────────────────────────┐
│              MySQL Database                     │
│  13 tables │ Views │ Triggers │ Procedures      │
└─────────────────────────────────────────────────┘
```

---

## 7. System Design

### 7.1 ER Diagram (Textual)

```
DEPARTMENT ──< TEACHER
DEPARTMENT ──< STUDENT
DEPARTMENT ──< SUBJECT
DEPARTMENT ──< CLASS

TEACHER ──< TEACHER_SUBJECT >── SUBJECT
TEACHER_SUBJECT ──< CLASS

STUDENT >── STUDENT_CLASS ──< CLASS

TEACHER ──< LECTURE ──> SUBJECT
LECTURE ──> CLASS
LECTURE ──< QR_TOKEN

STUDENT ──< ATTENDANCE >── LECTURE
ATTENDANCE ──> QR_TOKEN
```

### 7.2 Database Schema (Key Tables)

**teacher** (teacher_id PK, employee_id UQ, first_name, last_name, email UQ, password_hash, department_id FK, designation, is_active)

**student** (student_id PK, enrollment_no UQ, first_name, last_name, email UQ, password_hash, department_id FK, semester, year_of_study, is_active)

**lecture** (lecture_id PK, subject_id FK, teacher_id FK, class_id FK, lecture_date, start_time, status ENUM)

**qr_token** (token_id PK, lecture_id FK, token_value UQ, qr_data TEXT, generated_at, expires_at, is_expired)

**attendance** (attendance_id PK, student_id FK, lecture_id FK, token_id FK, marked_at, status ENUM, UNIQUE(student_id, lecture_id))

### 7.3 QR Code Flow

```
Teacher clicks "Generate QR"
       │
       ▼
Create Lecture record in DB
       │
       ▼
Generate UUID token (32 chars)
       │
       ▼
Build JSON payload:
{token, lectureId, subjectId, teacherId, classId, timestamp}
       │
       ▼
Encode payload to QR image (ZXing, 400×400px)
       │
       ▼
Store token in qr_token table with expires_at = NOW() + 30s
       │
       ▼
Display QR + 30s countdown timer in UI
       │
Student scans QR (image file)
       │
       ▼
Decode image → extract payload → extract token UUID
       │
       ▼
Validate token: exists? not expired? not used?
       │
       ▼
Check: attendance already recorded for this student+lecture?
       │
       ▼
Insert attendance record → trigger updates summary table
       │
       ▼
Show success confirmation
```

---

## 8. Modules

### Module 1: Authentication
- BCrypt password hashing (cost factor 12)
- Login via email or ID
- Session management
- Last-login timestamp update

### Module 2: Teacher Dashboard
- Stat cards: total students, today's lectures, avg attendance
- Today's schedule table
- Quick action buttons

### Module 3: QR Generation
- Subject + class + topic selection
- Configurable expiry (10–300 seconds)
- Live countdown with colour change (green→yellow→red)
- One QR per lecture (previous tokens expire on new generation)

### Module 4: Student Management
- Search by name/email/enrollment
- Add/Edit/Delete with form validation
- Class enrollment

### Module 5: Attendance Reporting
- Filter by class and date range
- Status colour-coding (green=present, red=absent, yellow=late)
- Percentage table with pass/fail flags

### Module 6: Reports & Charts
- Attendance percentage bar chart (JFreeChart)
- Pass/fail distribution pie chart
- PDF export (iText 7) with branded header
- Excel export (Apache POI) with colour formatting

### Module 7: Student Portal
- Dashboard with overall attendance percentage progress bar
- QR scan via image file selection
- Fallback: manual token paste
- Attendance history with colour-coding

---

## 9. Testing

### 9.1 Unit Tests (Manual)

| Test Case | Input | Expected | Result |
|-----------|-------|----------|--------|
| Valid teacher login | EMP001 / Teacher@123 | Dashboard opens | ✅ Pass |
| Wrong password | EMP001 / wrong | Error message | ✅ Pass |
| QR expiry | Token after 30s | Attendance rejected | ✅ Pass |
| Duplicate attendance | Same QR scanned twice | "Already recorded" | ✅ Pass |
| Add student | Valid form data | Student added | ✅ Pass |
| Invalid enrollment | Blank field | Validation error | ✅ Pass |
| PDF export | Load report → Export | File created | ✅ Pass |
| Excel export | Load report → Export | File created | ✅ Pass |

### 9.2 Edge Cases Tested
- QR code with expired token
- Student scanning QR from wrong class
- Teacher generating QR without selecting subject (validation)
- Database connection failure (error dialog shown)

---

## 10. Advantages

1. **Time-saving**: Attendance in seconds vs. 5–10 minutes for roll-call
2. **Anti-proxy**: 30-second QR expiry makes proxy attendance very difficult
3. **Accuracy**: No human error in recording
4. **Instant reports**: PDF/Excel available immediately
5. **Student transparency**: Students see their percentage in real-time
6. **Scalable**: Connection pool supports multiple concurrent users
7. **Secure**: BCrypt hashing; SQL injection-free (PreparedStatement)

---

## 11. Limitations

1. Desktop application only (no mobile native app)
2. QR scan requires saving an image (no live camera)
3. No email notification module (architecture ready)
4. Single-institution design

---

## 12. Future Scope

1. **Android/iOS companion app** with live camera QR scanning
2. **Email/SMS alerts** for low attendance (<75%)
3. **Geo-fencing**: Verify student is within campus GPS boundary
4. **Face recognition** as secondary anti-proxy measure
5. **Cloud deployment**: Move from desktop to web (Spring Boot + React)
6. **AI analytics**: Predict students at risk of failing attendance

---

## 13. Conclusion

The QR Code Based Attendance Management System successfully addresses the key problems of manual attendance tracking in colleges. It is a complete, production-ready application following MVC architecture, implementing all standard software engineering practices including OOP principles, proper exception handling, database normalisation, and secure password management. The system provides measurable improvements in efficiency and accuracy while laying a solid foundation for future enhancements.

---

## 14. References

1. Google ZXing Library — https://github.com/zxing/zxing
2. iText PDF Documentation — https://itextpdf.com/en/resources/api-documentation
3. Apache POI Documentation — https://poi.apache.org/apidocs/
4. JFreeChart Documentation — http://www.jfree.org/jfreechart/api/javadoc/
5. FlatLaf Look and Feel — https://www.formdev.com/flatlaf/
6. BCrypt at.favre.lib — https://github.com/patrickfav/bcrypt
7. MySQL 8.0 Reference Manual — https://dev.mysql.com/doc/
8. Java SE 17 Documentation — https://docs.oracle.com/en/java/javase/17/

---

*Document prepared for: Final Year B.Tech Project Evaluation*  
*Department of Computer Science & Engineering*  
*Academic Year 2024–2025*
