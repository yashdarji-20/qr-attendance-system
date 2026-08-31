package com.attendance.view;

import com.attendance.dao.ClassRoomDAO;
import com.attendance.dao.SubjectDAO;
import com.attendance.model.Attendance;
import com.attendance.model.ClassRoom;
import com.attendance.model.Subject;
import com.attendance.model.Teacher;
import com.attendance.service.AttendanceService;
import com.attendance.utils.UITheme;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Displays attendance records with filters.
 */
public class ViewAttendancePanel extends JPanel {

    private final Teacher           teacher;
    private final AttendanceService attendanceService = new AttendanceService();
    private final SubjectDAO        subjectDAO        = new SubjectDAO();
    private final ClassRoomDAO      classRoomDAO      = new ClassRoomDAO();

    private JComboBox<ClassItem>    classCombo;
    private JComboBox<String>       periodCombo;
    private JTable                  table;
    private DefaultTableModel       tableModel;
    private JLabel                  summaryLabel;

    private static final String[] COLS =
        {"Enroll No", "Student", "Subject", "Date", "Time", "Status", "Marked By"};

    public ViewAttendancePanel(Teacher teacher) {
        this.teacher = teacher;
        setBackground(UITheme.BG_MAIN);
        setLayout(new BorderLayout());
        buildUI();
        loadCombos();
    }

    private void buildUI() {
        JPanel outer = new JPanel(new BorderLayout(0, 16));
        outer.setBackground(UITheme.BG_MAIN);
        outer.setBorder(new EmptyBorder(24, 24, 24, 24));

        // Header
        JLabel heading = new JLabel("Attendance Records");
        heading.setFont(UITheme.FONT_HEADING);
        heading.setForeground(UITheme.PRIMARY);
        outer.add(heading, BorderLayout.NORTH);

        // Filter bar
        JPanel filterCard = UITheme.cardPanel();
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 8));

        filterCard.add(filterLabel("Class:"));
        classCombo = new JComboBox<>();
        UITheme.styleComboBox(classCombo);
        filterCard.add(classCombo);

        filterCard.add(filterLabel("Period:"));
        periodCombo = new JComboBox<>(new String[]{"Today", "This Week", "This Month", "All"});
        UITheme.styleComboBox(periodCombo);
        filterCard.add(periodCombo);

        UITheme.RoundedButton loadBtn = UITheme.primaryButton("Load Attendance");
        loadBtn.setPreferredSize(new Dimension(160, 38));
        loadBtn.addActionListener(e -> loadAttendance());
        filterCard.add(loadBtn);

        outer.add(filterCard, BorderLayout.CENTER);

        // Table
        tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    // Colour by status
                    String status = (String) tableModel.getValueAt(row, 5);
                    if ("PRESENT".equals(status))
                        c.setBackground(new Color(0xE8F8F0));
                    else if ("ABSENT".equals(status))
                        c.setBackground(new Color(0xFDE8E8));
                    else if ("LATE".equals(status))
                        c.setBackground(new Color(0xFEF9E7));
                    else
                        c.setBackground(row % 2 == 0 ? Color.WHITE : UITheme.BG_TABLE_ALT);
                }
                return c;
            }
        };
        UITheme.styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        JPanel tableCard = UITheme.cardPanel();
        tableCard.setLayout(new BorderLayout());

        // Summary row
        summaryLabel = new JLabel("Select a class and period, then click Load.");
        summaryLabel.setFont(UITheme.FONT_SMALL);
        summaryLabel.setForeground(UITheme.TEXT_MUTED);
        tableCard.add(summaryLabel, BorderLayout.NORTH);
        tableCard.add(scroll,       BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 16));
        bottom.setBackground(UITheme.BG_MAIN);
        bottom.add(tableCard, BorderLayout.CENTER);
        outer.add(bottom, BorderLayout.SOUTH);

        add(outer, BorderLayout.CENTER);
    }

    private void loadCombos() {
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                List<ClassRoom> classes = classRoomDAO.findByTeacher(teacher.getTeacherId());
                SwingUtilities.invokeLater(() -> {
                    classCombo.addItem(new ClassItem(null, "All Classes", 0));
                    for (ClassRoom c : classes) classCombo.addItem(
                            new ClassItem(c, c.getClassName(), c.getClassId()));
                });
                return null;
            }
        };
        w.execute();
    }

    private void loadAttendance() {
        ClassItem ci = (ClassItem) classCombo.getSelectedItem();
        String period = (String) periodCombo.getSelectedItem();

        LocalDate from, to;
        to = LocalDate.now();
        from = switch (period) {
            case "Today"     -> LocalDate.now();
            case "This Week" -> LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
            case "This Month"-> LocalDate.now().withDayOfMonth(1);
            default          -> LocalDate.now().minusYears(1);
        };

        final LocalDate finalFrom = from;
        int classId = (ci != null && ci.id > 0) ? ci.id : -1;

        SwingWorker<List<Attendance>, Void> w = new SwingWorker<>() {
            @Override protected List<Attendance> doInBackground() throws Exception {
                if (classId > 0) {
                    return attendanceService.getAttendanceByDateRange(classId, finalFrom, to);
                }
                return List.of();
            }
            @Override protected void done() {
                try {
                    List<Attendance> list = get();
                    tableModel.setRowCount(0);
                    for (Attendance a : list) {
                        tableModel.addRow(new Object[]{
                            a.getEnrollmentNo(),
                            a.getStudentName(),
                            a.getSubjectName(),
                            a.getLectureDate(),
                            a.getStartTime(),
                            a.getStatus().name(),
                            a.getMarkedBy() != null ? a.getMarkedBy().name() : "—"
                        });
                    }
                    summaryLabel.setText("Showing " + list.size() + " attendance records");
                } catch (Exception ex) {
                    summaryLabel.setText("Error loading: " + ex.getMessage());
                }
            }
        };
        w.execute();
    }

    private JLabel filterLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_LABEL);
        l.setForeground(UITheme.TEXT_PRIMARY);
        return l;
    }

    private record ClassItem(ClassRoom c, String display, int id) {
        @Override public String toString() { return display; }
    }
}
