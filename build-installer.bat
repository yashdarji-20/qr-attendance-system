@echo off
echo ============================================
echo  QR Attendance System - Build Installer
echo ============================================

cd /d "%~dp0"

echo.
echo [1/3] Building JAR...
call mvn clean package -q
if errorlevel 1 (
    echo ERROR: Maven build failed!
    pause
    exit /b 1
)
echo     Done!

echo.
echo [2/3] Creating installer...

if not exist "installer-input" mkdir installer-input
if not exist "installer-output" mkdir installer-output
copy "target\AttendanceSystem.jar" "installer-input\" >nul

jpackage ^
  --type exe ^
  --name "QR Attendance System" ^
  --vendor "DY Patil School of Biotechnology" ^
  --app-version "1.0.0" ^
  --description "QR Code Based Attendance Management System" ^
  --input installer-input ^
  --main-jar AttendanceSystem.jar ^
  --main-class com.attendance.Main ^
  --win-menu ^
  --win-shortcut ^
  --win-dir-chooser ^
  --win-menu-group "QR Attendance" ^
  --dest installer-output ^
  --java-options "-Xms256m" ^
  --java-options "-Xmx512m"

if errorlevel 1 (
    echo.
    echo ERROR: jpackage failed!
    pause
    exit /b 1
)

echo     Done!
echo.
echo ============================================
echo  SUCCESS!
echo  Installer is in: installer-output\
echo  File: QR Attendance System-1.0.0.exe
echo ============================================
echo.
pause
