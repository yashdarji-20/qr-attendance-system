package com.attendance.view;

import com.attendance.dao.ClassRoomDAO;
import com.attendance.dao.SubjectDAO;
import com.attendance.model.ClassRoom;
import com.attendance.model.Subject;
import com.attendance.model.Teacher;
import com.attendance.reports.ExcelReportGenerator;
import com.attendance.reports.PDFReportGenerator;
import com.attendance.service.AttendanceService;
import com.attendance.utils.UITheme;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Reports panel — attendance percentage table, charts, PDF/Excel export.
 */
public class ReportsPanel extends JPanel {

    private final Teacher           teacher;
    private final AttendanceService attendanceService = new AttendanceService();
    private final SubjectDAO        subjectDAO        = new SubjectDAO();
    private final ClassRoomDAO      classRoomDAO      = new ClassRoomDAO();

    private JComboBox<SubjectItem> subjectCombo;
    private JComboBox<ClassItem>   classCombo;
    private JTable                 table;
    private DefaultTableModel      tableModel;
    private JPanel                 chartHolder;

    private static final String[] COLS =
        {"Enroll No", "Student Name", "Total Lectures", "Attended", "Percentage", "Status"};

    public ReportsPanel(Teacher teacher) {
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

        outer.add(headerLabel("Attendance Reports"), BorderLayout.NORTH);

        // Filter card
        JPanel filterCard = UITheme.cardPanel();
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 8));
        filterCard.add(lbl("Subject:"));
        subjectCombo = new JComboBox<>(); UITheme.styleComboBox(subjectCombo);
        filterCard.add(subjectCombo);
        filterCard.add(lbl("Class:"));
        classCombo = new JComboBox<>();   UITheme.styleComboBox(classCombo);
        filterCard.add(classCombo);

        UITheme.RoundedButton loadBtn = UITheme.primaryButton("Load Report");
        loadBtn.setPreferredSize(new Dimension(140, 38));
        loadBtn.addActionListener(e -> loadReport());
        filterCard.add(loadBtn);

        UITheme.RoundedButton pdfBtn = UITheme.dangerButton("📄 Export PDF");
        pdfBtn.setPreferredSize(new Dimension(140, 38));
        pdfBtn.addActionListener(e -> exportPDF());
        filterCard.add(pdfBtn);

        UITheme.RoundedButton xlsBtn = UITheme.successButton("📊 Export Excel");
        xlsBtn.setPreferredSize(new Dimension(150, 38));
        xlsBtn.addActionListener(e -> exportExcel());
        filterCard.add(xlsBtn);

        outer.add(filterCard, BorderLayout.CENTER);

        // Split: table left, chart right
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(560);
        split.setBorder(BorderFactory.createEmptyBorder());

        // Table card
        tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    double pct = 0;
                    Object v = tableModel.getValueAt(row, 4);
                    if (v instanceof Double d) pct = d;
                    c.setBackground(pct < 75
                        ? new Color(0xFDE8E8)
                        : pct >= 90
                            ? new Color(0xE8F8F0)
                            : (row % 2 == 0 ? Color.WHITE : UITheme.BG_TABLE_ALT));
                }
                return c;
            }
        };
        UITheme.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        JPanel tableCard = UITheme.cardPanel();
        tableCard.setLayout(new BorderLayout());
        tableCard.add(scroll, BorderLayout.CENTER);
        split.setLeftComponent(tableCard);

        // Chart panel
        chartHolder = new JPanel(new BorderLayout());
        chartHolder.setBackground(Color.WHITE);
        chartHolder.setBorder(new CompoundBorder(
            new LineBorder(UITheme.BORDER_COLOR), new EmptyBorder(8, 8, 8, 8)));
        JLabel chartPlaceholder = new JLabel("Load a report to see charts",
                SwingConstants.CENTER);
        chartPlaceholder.setFont(UITheme.FONT_BODY);
        chartPlaceholder.setForeground(UITheme.TEXT_MUTED);
        chartHolder.add(chartPlaceholder, BorderLayout.CENTER);
        split.setRightComponent(chartHolder);

        JPanel bottomArea = new JPanel(new BorderLayout());
        bottomArea.setBackground(UITheme.BG_MAIN);
        bottomArea.add(split, BorderLayout.CENTER);
        outer.add(bottomArea, BorderLayout.SOUTH);

        add(outer, BorderLayout.CENTER);
    }

    // ---- Load & display report --------------------------------------

    private void loadReport() {
        SubjectItem si = (SubjectItem) subjectCombo.getSelectedItem();
        ClassItem   ci = (ClassItem)   classCombo.getSelectedItem();
        if (si == null || ci == null) {
            JOptionPane.showMessageDialog(this, "Please select subject and class.",
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SwingWorker<List<Object[]>, Void> w = new SwingWorker<>() {
            @Override protected List<Object[]> doInBackground() throws Exception {
                return attendanceService.getAttendancePercentage(si.id, ci.id);
            }
            @Override protected void done() {
                try {
                    List<Object[]> rows = get();
                    tableModel.setRowCount(0);
                    DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
                    DefaultPieDataset<String> pieDataset = new DefaultPieDataset<>();
                    int below75 = 0, above75 = 0;

                    for (Object[] row : rows) {
                        double pct = (Double) row[5];
                        String status = pct >= 75 ? "✅ OK" : "⚠ Low";
                        if (pct < 75) below75++; else above75++;
                        tableModel.addRow(new Object[]{
                            row[1], row[2], row[3], row[4],
                            String.format("%.1f%%", pct), status
                        });
                        barDataset.addValue(pct, "Attendance %", (String) row[2]);
                    }

                    pieDataset.setValue("≥75% (OK)",    above75);
                    pieDataset.setValue("<75% (Low)",   below75);

                    // Build charts
                    JFreeChart barChart = ChartFactory.createBarChart(
                        "Attendance by Student", "Student", "Percentage (%)",
                        barDataset, PlotOrientation.VERTICAL, false, true, false);
                    barChart.getPlot().setBackgroundPaint(Color.WHITE);

                    JFreeChart pieChart = ChartFactory.createPieChart(
                        "Attendance Distribution", pieDataset, true, true, false);

                    // Display bar chart (scroll if many students)
                    JSplitPane chartSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
                    chartSplit.setTopComponent   (new ChartPanel(barChart));
                    chartSplit.setBottomComponent(new ChartPanel(pieChart));
                    chartSplit.setDividerLocation(200);

                    chartHolder.removeAll();
                    chartHolder.add(chartSplit, BorderLayout.CENTER);
                    chartHolder.revalidate();
                    chartHolder.repaint();

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ReportsPanel.this,
                            "Error loading report: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }

    // ---- Export -----------------------------------------------------

    private void exportPDF() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("attendance_report.pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            SubjectItem si = (SubjectItem) subjectCombo.getSelectedItem();
            ClassItem   ci = (ClassItem)   classCombo.getSelectedItem();
            List<Object[]> data = si != null && ci != null
                    ? attendanceService.getAttendancePercentage(si.id, ci.id)
                    : List.of();
            PDFReportGenerator.generateAttendanceReport(
                    data, fc.getSelectedFile().getAbsolutePath(),
                    si != null ? si.name : "All Subjects",
                    ci != null ? ci.name : "All Classes",
                    teacher.getFullName());
            JOptionPane.showMessageDialog(this, "PDF exported successfully!",
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "PDF export failed: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportExcel() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("attendance_report.xlsx"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            SubjectItem si = (SubjectItem) subjectCombo.getSelectedItem();
            ClassItem   ci = (ClassItem)   classCombo.getSelectedItem();
            List<Object[]> data = si != null && ci != null
                    ? attendanceService.getAttendancePercentage(si.id, ci.id)
                    : List.of();
            ExcelReportGenerator.generateAttendanceReport(
                    data, fc.getSelectedFile().getAbsolutePath(),
                    si != null ? si.name : "All",
                    ci != null ? ci.name : "All");
            JOptionPane.showMessageDialog(this, "Excel exported successfully!",
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Excel export failed: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---- Data loading -----------------------------------------------

    private void loadCombos() {
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                List<Subject>   subjects = subjectDAO.findByTeacher(teacher.getTeacherId());
                List<ClassRoom> classes  = classRoomDAO.findByTeacher(teacher.getTeacherId());
                SwingUtilities.invokeLater(() -> {
                    for (Subject s   : subjects) subjectCombo.addItem(
                            new SubjectItem(s.getSubjectId(), s.getSubjectName()));
                    for (ClassRoom c : classes)  classCombo.addItem(
                            new ClassItem(c.getClassId(), c.getClassName()));
                });
                return null;
            }
        };
        w.execute();
    }

    // ---- Helpers ----------------------------------------------------

    private JLabel headerLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_HEADING);
        l.setForeground(UITheme.PRIMARY);
        return l;
    }

    private JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_LABEL);
        l.setForeground(UITheme.TEXT_PRIMARY);
        return l;
    }

    private record SubjectItem(int id, String name) {
        @Override public String toString() { return name; }
    }

    private record ClassItem(int id, String name) {
        @Override public String toString() { return name; }
    }
}
