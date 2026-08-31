package com.attendance.view;

import com.attendance.dao.*;
import com.attendance.model.*;
import com.attendance.service.*;
import com.attendance.utils.UITheme;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Teacher Dashboard — main window after teacher login.
 * Layout: fixed sidebar + dynamic content panel.
 */
public class TeacherDashboardView extends JFrame {

    private final Teacher teacher;
    private JPanel contentPanel;
    private final CardLayout cardLayout = new CardLayout();

    // Sidebar nav items (label -> card name)
    private static final String[][] NAV_ITEMS = {
        {"Dashboard",     "DASHBOARD"},
        {"Students",      "STUDENTS"},
        {"Generate QR",   "GENERATE_QR"},
        {"Attendance",    "ATTENDANCE"},
        {"Reports",       "REPORTS"},
        {"Subjects",      "SUBJECTS"},
        {"Profile",       "PROFILE"}
    };

    // Sub-panels
    private TeacherHomePanel    homePanel;
    private ManageStudentsPanel studentsPanel;
    private GenerateQRPanel     generateQRPanel;
    private ViewAttendancePanel attendancePanel;
    private ReportsPanel        reportsPanel;

    private final UITheme.RoundedButton[] navButtons;

    public TeacherDashboardView(Teacher teacher) {
        this.teacher    = teacher;
        this.navButtons = new UITheme.RoundedButton[NAV_ITEMS.length];
        initUI();
        setVisible(true);
        showCard("DASHBOARD");
    }

    private void initUI() {
        setTitle("QR Attendance System — Teacher: " + teacher.getFullName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 780);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1024, 680));

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(),     BorderLayout.WEST);
        root.add(buildMainArea(),    BorderLayout.CENTER);
        setContentPane(root);
    }

    // ---- Sidebar ----------------------------------------------------

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(UITheme.BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(UITheme.SIDEBAR_WIDTH, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        // Logo section
        JPanel logo = new JPanel();
        logo.setBackground(UITheme.PRIMARY_DARK);
        logo.setMaximumSize(new Dimension(UITheme.SIDEBAR_WIDTH, 80));
        logo.setPreferredSize(new Dimension(UITheme.SIDEBAR_WIDTH, 80));
        logo.setLayout(new FlowLayout(FlowLayout.CENTER, 12, 22));
        JLabel logoText = new JLabel("📋 QR Attend");
        logoText.setFont(new Font("Segoe UI", Font.BOLD, 16));
        logoText.setForeground(Color.WHITE);
        logo.add(logoText);
        sidebar.add(logo);

        // Teacher info card
        JPanel infoCard = new JPanel();
        infoCard.setBackground(UITheme.PRIMARY_LIGHT);
        infoCard.setMaximumSize(new Dimension(UITheme.SIDEBAR_WIDTH, 80));
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        infoCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel nameLabel = new JLabel("Prof. " + teacher.getFirstName() + " " + teacher.getLastName());
        nameLabel.setFont(UITheme.FONT_LABEL);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel roleLabel = new JLabel("Teacher  •  " + teacher.getEmployeeId());
        roleLabel.setFont(UITheme.FONT_SMALL);
        roleLabel.setForeground(new Color(180, 210, 255));
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoCard.add(nameLabel);
        infoCard.add(Box.createVerticalStrut(4));
        infoCard.add(roleLabel);
        sidebar.add(infoCard);

        sidebar.add(Box.createVerticalStrut(16));

        // Navigation buttons
        for (int i = 0; i < NAV_ITEMS.length; i++) {
            String label    = NAV_ITEMS[i][0];
            String cardName = NAV_ITEMS[i][1];
            UITheme.RoundedButton btn = buildNavButton(label, cardName);
            navButtons[i] = btn;
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(4));
        }

        sidebar.add(Box.createVerticalGlue());

        // Logout button
        UITheme.RoundedButton logoutBtn = new UITheme.RoundedButton("Logout", UITheme.DANGER, Color.WHITE);
        logoutBtn.setMaximumSize(new Dimension(UITheme.SIDEBAR_WIDTH - 32, 42));
        logoutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutBtn.addActionListener(e -> doLogout());
        sidebar.add(logoutBtn);
        sidebar.add(Box.createVerticalStrut(20));

        return sidebar;
    }

    private UITheme.RoundedButton buildNavButton(String label, String cardName) {
        UITheme.RoundedButton btn = new UITheme.RoundedButton(label,
                UITheme.PRIMARY_LIGHT, Color.WHITE) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isArmed() || getClientProperty("selected") != null
                         ? UITheme.ACCENT : UITheme.PRIMARY_LIGHT;
                g2.setColor(bg);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setMaximumSize(new Dimension(UITheme.SIDEBAR_WIDTH - 16, 44));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(0, 20, 0, 0));
        btn.addActionListener(e -> {
            showCard(cardName);
            highlightNav(btn);
        });
        return btn;
    }

    private void highlightNav(UITheme.RoundedButton selected) {
        for (UITheme.RoundedButton b : navButtons) {
            b.putClientProperty("selected", null);
            b.repaint();
        }
        selected.putClientProperty("selected", Boolean.TRUE);
        selected.repaint();
    }

    // ---- Main area --------------------------------------------------

    private JPanel buildMainArea() {
        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setPreferredSize(new Dimension(0, UITheme.TOPBAR_HEIGHT));
        topBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR),
            new EmptyBorder(0, 24, 0, 24)
        ));
        JLabel pageTitle = new JLabel("Teacher Dashboard");
        pageTitle.setFont(UITheme.FONT_SUBHEADING);
        pageTitle.setForeground(UITheme.PRIMARY);
        topBar.add(pageTitle, BorderLayout.WEST);

        JLabel dateLabel = new JLabel(LocalDate.now().toString() + "  |  " + teacher.getDesignation());
        dateLabel.setFont(UITheme.FONT_SMALL);
        dateLabel.setForeground(UITheme.TEXT_SECONDARY);
        topBar.add(dateLabel, BorderLayout.EAST);

        // Content area with CardLayout
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(UITheme.BG_MAIN);

        homePanel       = new TeacherHomePanel(teacher);
        studentsPanel   = new ManageStudentsPanel(teacher);
        generateQRPanel = new GenerateQRPanel(teacher);
        attendancePanel = new ViewAttendancePanel(teacher);
        reportsPanel    = new ReportsPanel(teacher);

        contentPanel.add(homePanel,       "DASHBOARD");
        contentPanel.add(studentsPanel,   "STUDENTS");
        contentPanel.add(generateQRPanel, "GENERATE_QR");
        contentPanel.add(attendancePanel, "ATTENDANCE");
        contentPanel.add(reportsPanel,    "REPORTS");
        contentPanel.add(buildSimpleCard("Subjects — Coming soon"), "SUBJECTS");
        contentPanel.add(buildProfilePanel(), "PROFILE");

        JPanel main = new JPanel(new BorderLayout());
        main.add(topBar,       BorderLayout.NORTH);
        main.add(new JScrollPane(contentPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        return main;
    }

    private void showCard(String name) {
        cardLayout.show(contentPanel, name);
    }

    private JPanel buildSimpleCard(String msg) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(UITheme.BG_MAIN);
        JLabel l = new JLabel(msg);
        l.setFont(UITheme.FONT_HEADING);
        l.setForeground(UITheme.TEXT_SECONDARY);
        p.add(l);
        return p;
    }

    private JPanel buildProfilePanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(UITheme.BG_MAIN);

        JPanel card = UITheme.cardPanel();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(500, 350));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0;
        g.fill  = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(8, 8, 8, 8);
        g.weightx = 1;

        card.add(sectionTitle("My Profile"), g);
        g.gridy++;
        card.add(infoRow("Name",        teacher.getFullName()), g);
        g.gridy++;
        card.add(infoRow("Employee ID", teacher.getEmployeeId()), g);
        g.gridy++;
        card.add(infoRow("Email",       teacher.getEmail()), g);
        g.gridy++;
        card.add(infoRow("Phone",       teacher.getPhone()), g);
        g.gridy++;
        card.add(infoRow("Designation", teacher.getDesignation()), g);

        outer.add(card);
        return outer;
    }

    private JPanel infoRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(UITheme.FONT_LABEL);
        lbl.setForeground(UITheme.TEXT_SECONDARY);
        lbl.setPreferredSize(new Dimension(130, 30));
        JLabel val = new JLabel(value != null ? value : "—");
        val.setFont(UITheme.FONT_BODY);
        val.setForeground(UITheme.TEXT_PRIMARY);
        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_HEADING);
        l.setForeground(UITheme.PRIMARY);
        l.setBorder(new EmptyBorder(0, 0, 12, 0));
        return l;
    }

    private void doLogout() {
        int choice = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to logout?", "Confirm Logout",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            new AuthService().logoutTeacher();
            dispose();
            SwingUtilities.invokeLater(LoginView::new);
        }
    }
}
