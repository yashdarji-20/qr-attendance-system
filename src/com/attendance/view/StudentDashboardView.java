package com.attendance.view;

import com.attendance.model.Attendance;
import com.attendance.model.Student;
import com.attendance.service.AttendanceService;
import com.attendance.service.AuthService;
import com.attendance.utils.UITheme;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/**
 * Student Dashboard — live QR scan, attendance history, profile.
 */
public class StudentDashboardView extends JFrame {

    private final Student           student;
    private final AttendanceService attendanceService = new AttendanceService();
    private final AuthService       authService       = new AuthService();

    private JPanel      contentPanel;
    private final CardLayout cardLayout = new CardLayout();

    private ScanQRPanel scanQRPanel;           // live webcam scanner

    // ---- Attendance history table
    private JTable            histTable;
    private DefaultTableModel histModel;

    // ---- Home card percentage widgets
    private JLabel       pctLabel;
    private JProgressBar pctBar;

    public StudentDashboardView(Student student) {
        this.student = student;
        initUI();
        setVisible(true);
        loadAttendanceHistory();
        updatePercentage();
    }

    // ---- Frame setup ------------------------------------------------

    private void initUI() {
        setTitle("QR Attendance — Student: " + student.getFullName());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                doLogout();
            }
        });
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 600));

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildMain(),    BorderLayout.CENTER);
        setContentPane(root);
    }

    // ---- Sidebar ----------------------------------------------------

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(UITheme.BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        // Logo strip
        JPanel logo = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 22));
        logo.setBackground(UITheme.PRIMARY_DARK);
        logo.setMaximumSize(new Dimension(210, 70));
        JLabel logoLbl = new JLabel("QR Attend");
        logoLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        logoLbl.setForeground(Color.WHITE);
        logo.add(logoLbl);
        sidebar.add(logo);

        // Student info strip
        JPanel info = new JPanel();
        info.setBackground(UITheme.PRIMARY_LIGHT);
        info.setMaximumSize(new Dimension(210, 80));
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(new EmptyBorder(10, 16, 10, 16));
        JLabel nameLbl = new JLabel(student.getFullName());
        nameLbl.setFont(UITheme.FONT_LABEL);
        nameLbl.setForeground(Color.WHITE);
        nameLbl.setAlignmentX(LEFT_ALIGNMENT);
        JLabel enrollLbl = new JLabel(student.getEnrollmentNo());
        enrollLbl.setFont(UITheme.FONT_SMALL);
        enrollLbl.setForeground(new Color(180, 210, 255));
        enrollLbl.setAlignmentX(LEFT_ALIGNMENT);
        info.add(nameLbl);
        info.add(Box.createVerticalStrut(4));
        info.add(enrollLbl);
        sidebar.add(info);
        sidebar.add(Box.createVerticalStrut(16));

        // Nav buttons
        String[][] navItems = {
            {"🏠  Dashboard",     "HOME"},
            {"📷  Scan QR Code",  "SCAN"},
            {"📋  My Attendance", "HISTORY"},
            {"👤  Profile",       "PROFILE"}
        };
        for (String[] item : navItems) {
            JButton btn = buildNavBtn(item[0], item[1]);
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(4));
        }

        sidebar.add(Box.createVerticalGlue());

        UITheme.RoundedButton logout = new UITheme.RoundedButton("Logout", UITheme.DANGER, Color.WHITE);
        logout.setMaximumSize(new Dimension(190, 42));
        logout.setAlignmentX(CENTER_ALIGNMENT);
        logout.addActionListener(e -> doLogout());
        sidebar.add(logout);
        sidebar.add(Box.createVerticalStrut(20));
        return sidebar;
    }

    private JButton buildNavBtn(String label, String card) {
        JButton btn = new JButton(label);
        btn.setFont(UITheme.FONT_BODY);
        btn.setForeground(Color.WHITE);
        btn.setBackground(UITheme.PRIMARY_LIGHT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setMaximumSize(new Dimension(194, 44));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(0, 24, 0, 0));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            // Stop camera when leaving scan panel
            if (!"SCAN".equals(card) && scanQRPanel != null) scanQRPanel.onHide();
            cardLayout.show(contentPanel, card);
        });
        return btn;
    }

    // ---- Main content area -----------------------------------------

    private JPanel buildMain() {
        scanQRPanel = new ScanQRPanel(student);

        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(UITheme.BG_MAIN);
        contentPanel.add(buildHomeCard(),    "HOME");
        contentPanel.add(scanQRPanel,        "SCAN");
        contentPanel.add(buildHistoryCard(), "HISTORY");
        contentPanel.add(buildProfileCard(), "PROFILE");

        JPanel main = new JPanel(new BorderLayout());
        main.add(contentPanel, BorderLayout.CENTER);
        return main;
    }

    // ---- Home card --------------------------------------------------

    private JPanel buildHomeCard() {
        JPanel p = new JPanel();
        p.setBackground(UITheme.BG_MAIN);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(28, 28, 28, 28));

        JLabel welcome = new JLabel("Welcome, " + student.getFirstName() + "! 🎓");
        welcome.setFont(UITheme.FONT_HEADING);
        welcome.setForeground(UITheme.PRIMARY);
        welcome.setAlignmentX(LEFT_ALIGNMENT);
        p.add(welcome);
        p.add(Box.createVerticalStrut(24));

        // Attendance overview card
        JPanel card = UITheme.cardPanel();
        card.setLayout(new GridBagLayout());
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        card.setAlignmentX(LEFT_ALIGNMENT);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.insets = new Insets(4, 4, 4, 4);

        pctLabel = new JLabel("—%");
        pctLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        pctLabel.setForeground(UITheme.PRIMARY);
        card.add(pctLabel, g);

        g.gridy = 1;
        JLabel pctDesc = new JLabel("Overall Attendance Percentage");
        pctDesc.setFont(UITheme.FONT_BODY);
        pctDesc.setForeground(UITheme.TEXT_SECONDARY);
        card.add(pctDesc, g);

        g.gridy = 2; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
        pctBar = new JProgressBar(0, 100);
        pctBar.setStringPainted(true);
        pctBar.setForeground(UITheme.SUCCESS);
        pctBar.setBackground(UITheme.BG_TABLE_ALT);
        pctBar.setPreferredSize(new Dimension(300, 20));
        card.add(pctBar, g);

        p.add(card);
        p.add(Box.createVerticalStrut(24));

        // Quick actions
        JPanel qRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        qRow.setOpaque(false);
        qRow.setAlignmentX(LEFT_ALIGNMENT);

        UITheme.RoundedButton scanBtn = UITheme.successButton("📷  Scan QR Code");
        scanBtn.addActionListener(e -> cardLayout.show(contentPanel, "SCAN"));

        UITheme.RoundedButton histBtn = UITheme.primaryButton("📋  View History");
        histBtn.addActionListener(e -> cardLayout.show(contentPanel, "HISTORY"));

        qRow.add(scanBtn);
        qRow.add(histBtn);
        p.add(qRow);
        return p;
    }

    // ---- Attendance history card ------------------------------------

    private JPanel buildHistoryCard() {
        JPanel outer = new JPanel(new BorderLayout(0, 16));
        outer.setBackground(UITheme.BG_MAIN);
        outer.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel heading = new JLabel("My Attendance History");
        heading.setFont(UITheme.FONT_HEADING);
        heading.setForeground(UITheme.PRIMARY);
        outer.add(heading, BorderLayout.NORTH);

        String[] cols = {"Date", "Subject", "Class", "Time", "Status"};
        histModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        histTable = new JTable(histModel) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    String status = (String) histModel.getValueAt(row, 4);
                    c.setBackground("PRESENT".equals(status)
                        ? new Color(0xE8F8F0)
                        : "ABSENT".equals(status)
                            ? new Color(0xFDE8E8)
                            : (row % 2 == 0 ? Color.WHITE : UITheme.BG_TABLE_ALT));
                }
                return c;
            }
        };
        UITheme.styleTable(histTable);

        JScrollPane scroll = new JScrollPane(histTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        JPanel tableCard = UITheme.cardPanel();
        tableCard.setLayout(new BorderLayout());
        tableCard.add(scroll, BorderLayout.CENTER);
        outer.add(tableCard, BorderLayout.CENTER);
        return outer;
    }

    // ---- Profile card -----------------------------------------------

    private JPanel buildProfileCard() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(UITheme.BG_MAIN);

        JPanel card = UITheme.cardPanel();
        card.setPreferredSize(new Dimension(460, 380));
        card.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 4, 6, 4);
        g.weightx = 1;

        JLabel title = new JLabel("My Profile");
        title.setFont(UITheme.FONT_HEADING);
        title.setForeground(UITheme.PRIMARY);
        card.add(title, g);

        String[][] rows = {
            {"Full Name",      student.getFullName()},
            {"Enrollment No",  student.getEnrollmentNo()},
            {"Email",          student.getEmail()},
            {"Phone",          student.getPhone() != null ? student.getPhone() : "—"},
            {"Department",     student.getDepartmentName() != null ? student.getDepartmentName() : "—"},
            {"Semester",       "Sem " + student.getSemester()},
        };
        for (String[] row : rows) {
            g.gridy++;
            JPanel rowPanel = new JPanel(new BorderLayout(12, 0));
            rowPanel.setBackground(Color.WHITE);
            JLabel lbl = new JLabel(row[0] + ":");
            lbl.setFont(UITheme.FONT_LABEL);
            lbl.setForeground(UITheme.TEXT_SECONDARY);
            lbl.setPreferredSize(new Dimension(140, 32));
            JLabel val = new JLabel(row[1]);
            val.setFont(UITheme.FONT_BODY);
            val.setForeground(UITheme.TEXT_PRIMARY);
            rowPanel.add(lbl, BorderLayout.WEST);
            rowPanel.add(val, BorderLayout.CENTER);
            card.add(rowPanel, g);
        }
        outer.add(card);
        return outer;
    }

    // ---- Data loading -----------------------------------------------

    public void loadAttendanceHistory() {
        SwingWorker<List<Attendance>, Void> w = new SwingWorker<>() {
            @Override protected List<Attendance> doInBackground() throws Exception {
                return attendanceService.getStudentAttendance(student.getStudentId());
            }
            @Override protected void done() {
                try {
                    List<Attendance> list = get();
                    if (histModel != null) {
                        histModel.setRowCount(0);
                        for (Attendance a : list) {
                            histModel.addRow(new Object[]{
                                a.getLectureDate(), a.getSubjectName(),
                                a.getClassName(),   a.getStartTime(),
                                a.getStatus().name()
                            });
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        };
        w.execute();
    }

    public void updatePercentage() {
        SwingWorker<Double, Void> w = new SwingWorker<>() {
            @Override protected Double doInBackground() throws Exception {
                return attendanceService.computeStudentOverallPercentage(student.getStudentId());
            }
            @Override protected void done() {
                try {
                    double pct = get();
                    if (pctLabel != null) pctLabel.setText(String.format("%.1f%%", pct));
                    if (pctBar   != null) {
                        pctBar.setValue((int) pct);
                        pctBar.setString(String.format("%.1f%%", pct));
                        pctBar.setForeground(pct < 75 ? UITheme.DANGER : UITheme.SUCCESS);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        };
        w.execute();
    }

    // ---- Logout -----------------------------------------------------

    private void doLogout() {
        if (scanQRPanel != null) scanQRPanel.onHide(); // release camera
        int ch = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?", "Confirm Logout",
                JOptionPane.YES_NO_OPTION);
        if (ch == JOptionPane.YES_OPTION) {
            authService.logoutStudent();
            dispose();
            SwingUtilities.invokeLater(LoginView::new);
        }
    }
}
