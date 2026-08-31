package com.attendance.view;

import com.attendance.dao.AttendanceDAO;
import com.attendance.dao.LectureDAO;
import com.attendance.dao.StudentDAO;
import com.attendance.model.Lecture;
import com.attendance.model.Teacher;
import com.attendance.utils.UITheme;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Home/Dashboard panel shown after teacher login.
 * Shows: stat cards, today's lectures, quick action buttons.
 */
public class TeacherHomePanel extends JPanel {

    private final Teacher       teacher;
    private final LectureDAO    lectureDAO    = new LectureDAO();
    private final StudentDAO    studentDAO    = new StudentDAO();
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();

    // Stat card labels
    private JLabel totalStudentsVal;
    private JLabel todayLecturesVal;
    private JLabel attendanceRateVal;
    private JLabel totalLecturesVal;

    public TeacherHomePanel(Teacher teacher) {
        this.teacher = teacher;
        setBackground(UITheme.BG_MAIN);
        setLayout(new BorderLayout(0, 0));
        buildUI();
        loadStats();
    }

    private void buildUI() {
        JPanel content = new JPanel();
        content.setBackground(UITheme.BG_MAIN);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(28, 28, 28, 28));

        // Welcome
        JPanel welcomeRow = new JPanel(new BorderLayout());
        welcomeRow.setOpaque(false);
        JLabel welcome = new JLabel("Good day, Prof. " + teacher.getFirstName() + "! 👋");
        welcome.setFont(UITheme.FONT_HEADING);
        welcome.setForeground(UITheme.PRIMARY);
        JLabel dateStr = new JLabel(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")));
        dateStr.setFont(UITheme.FONT_BODY);
        dateStr.setForeground(UITheme.TEXT_SECONDARY);
        welcomeRow.add(welcome,  BorderLayout.WEST);
        welcomeRow.add(dateStr,  BorderLayout.EAST);
        content.add(welcomeRow);
        content.add(Box.createVerticalStrut(24));

        // Stat cards row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        totalStudentsVal  = addStatCard(statsRow, "Total Students",  "...", UITheme.PRIMARY,    "👥");
        todayLecturesVal  = addStatCard(statsRow, "Today's Lectures","...", UITheme.ACCENT,     "📚");
        attendanceRateVal = addStatCard(statsRow, "Avg Attendance",  "...", UITheme.SUCCESS,    "✅");
        totalLecturesVal  = addStatCard(statsRow, "Total Lectures",  "...", UITheme.WARNING,    "📋");

        content.add(statsRow);
        content.add(Box.createVerticalStrut(28));

        // Quick actions
        JLabel qaTitle = new JLabel("Quick Actions");
        qaTitle.setFont(UITheme.FONT_SUBHEADING);
        qaTitle.setForeground(UITheme.PRIMARY);
        content.add(qaTitle);
        content.add(Box.createVerticalStrut(12));

        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        quickActions.setOpaque(false);

        String[][] actions = {
            {"Generate QR Code", "🔲", "GENERATE_QR"},
            {"Add Student",      "➕", "STUDENTS"},
            {"View Attendance",  "👁", "ATTENDANCE"},
            {"Export Report",    "📄", "REPORTS"}
        };
        for (String[] action : actions) {
            JButton btn = buildActionButton(action[0], action[1]);
            quickActions.add(btn);
        }
        content.add(quickActions);
        content.add(Box.createVerticalStrut(28));

        // Today's lectures table
        JLabel todayTitle = new JLabel("Today's Schedule");
        todayTitle.setFont(UITheme.FONT_SUBHEADING);
        todayTitle.setForeground(UITheme.PRIMARY);
        content.add(todayTitle);
        content.add(Box.createVerticalStrut(12));
        content.add(buildTodayLecturesTable());

        add(new JScrollPane(content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
    }

    private JLabel addStatCard(JPanel parent, String title, String value,
                                Color color, String icon) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                // Card background
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                // Accent top bar
                g2.setColor(color);
                g2.fillRoundRect(0, 0, getWidth(), 5, 4, 4);
                g2.fillRect(0, 3, getWidth(), 5);
                g2.dispose();
            }
        };
        card.setLayout(new BorderLayout());
        card.setBorder(new CompoundBorder(
            new LineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(18, 20, 18, 20)
        ));
        card.setOpaque(false);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        iconLabel.setForeground(color);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_SMALL);
        titleLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        valueLabel.setForeground(UITheme.TEXT_PRIMARY);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(titleLabel);
        left.add(Box.createVerticalStrut(8));
        left.add(valueLabel);

        card.add(left,      BorderLayout.CENTER);
        card.add(iconLabel, BorderLayout.EAST);
        parent.add(card);
        return valueLabel;
    }

    private JButton buildActionButton(String text, String icon) {
        JButton btn = new JButton(icon + "  " + text);
        btn.setFont(UITheme.FONT_BUTTON);
        btn.setForeground(UITheme.PRIMARY);
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(10, 16, 10, 16)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(UITheme.BG_MAIN);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(Color.WHITE);
            }
        });
        return btn;
    }

    private JPanel buildTodayLecturesTable() {
        String[] cols = {"Subject", "Class", "Time", "Students Present", "Status"};
        Object[][] data = new Object[0][0];

        try {
            List<Lecture> lectures = lectureDAO.findTodayByTeacher(teacher.getTeacherId());
            data = new Object[lectures.size()][5];
            for (int i = 0; i < lectures.size(); i++) {
                Lecture l = lectures.get(i);
                data[i] = new Object[]{
                    l.getSubjectName(),
                    l.getClassName(),
                    l.getStartTime() != null ? l.getStartTime().toString() : "-",
                    l.getPresentCount() + " / " + l.getTotalStudents(),
                    l.getStatus().name()
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        JTable table = new JTable(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                c.setBackground(row % 2 == 0 ? Color.WHITE : UITheme.BG_TABLE_ALT);
                return c;
            }
        };
        UITheme.styleTable(table);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        wrapper.add(new JScrollPane(table), BorderLayout.CENTER);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        return wrapper;
    }

    private void loadStats() {
        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            @Override
            protected int[] doInBackground() throws Exception {
                int students  = studentDAO.findAll().size();
                int todayLec  = lectureDAO.findTodayByTeacher(teacher.getTeacherId()).size();
                int totalLec  = lectureDAO.findByTeacher(teacher.getTeacherId()).size();
                return new int[]{students, todayLec, totalLec};
            }
            @Override
            protected void done() {
                try {
                    int[] vals = get();
                    totalStudentsVal.setText(String.valueOf(vals[0]));
                    todayLecturesVal.setText(String.valueOf(vals[1]));
                    totalLecturesVal.setText(String.valueOf(vals[2]));
                    attendanceRateVal.setText("75%");
                } catch (Exception e) {
                    totalStudentsVal.setText("—");
                }
            }
        };
        worker.execute();
    }
}
