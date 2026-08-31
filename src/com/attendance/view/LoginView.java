package com.attendance.view;

import com.attendance.model.Student;
import com.attendance.model.Teacher;
import com.attendance.service.AuthService;
import com.attendance.utils.UITheme;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Professional Login Screen.
 * Left panel: branding/illustration. Right panel: login form.
 * Supports both Teacher and Student login via tab selection.
 */
public class LoginView extends JFrame {

    // UI components
    private JTabbedPane tabPane;
    // Teacher login
    private JTextField  teacherIdField;
    private JPasswordField teacherPassField;
    private JButton     teacherLoginBtn;
    private JLabel      teacherMsgLabel;
    // Student login
    private JTextField  studentIdField;
    private JPasswordField studentPassField;
    private JButton     studentLoginBtn;
    private JLabel      studentMsgLabel;

    private final AuthService authService;

    public LoginView() {
        this.authService = new AuthService();
        initUI();
        setVisible(true);
    }

    private void initUI() {
        setTitle("QR Attendance System — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 620);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildLeftPanel(),  BorderLayout.WEST);
        root.add(buildRightPanel(), BorderLayout.CENTER);
        setContentPane(root);
    }

    // ---- Left branding panel ----------------------------------------

    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                // Gradient background
                GradientPaint gp = new GradientPaint(
                    0, 0, UITheme.PRIMARY_DARK,
                    0, getHeight(), UITheme.PRIMARY_LIGHT
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Decorative circles
                g2.setColor(new Color(255, 255, 255, 20));
                g2.fillOval(-60, -60, 220, 220);
                g2.fillOval(80, 350, 280, 280);
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillOval(200, 80, 160, 160);

                g2.dispose();
            }
        };
        panel.setPreferredSize(new Dimension(380, 0));
        panel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(0, 30, 8, 30);

        // QR icon drawn with Swing
        JPanel iconPanel = new QRIconPanel();
        iconPanel.setOpaque(false);
        iconPanel.setPreferredSize(new Dimension(100, 100));
        gbc.gridy++; panel.add(iconPanel, gbc);

        // App name
        JLabel appName = new JLabel("QR Attendance");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 26));
        appName.setForeground(Color.WHITE);
        gbc.gridy++; panel.add(appName, gbc);

        // Sub-title
        JLabel sub = new JLabel("Management System");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        sub.setForeground(new Color(180, 210, 255));
        gbc.gridy++; panel.add(sub, gbc);

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255,255,255,60));
        sep.setPreferredSize(new Dimension(220, 1));
        gbc.gridy++; gbc.insets = new Insets(16, 30, 16, 30);
        panel.add(sep, gbc);

        // Tagline
        String[] lines = {
            "✓  Generate QR codes instantly",
            "✓  Mark attendance in seconds",
            "✓  Real-time analytics",
            "✓  Export PDF & Excel reports"
        };
        gbc.insets = new Insets(4, 40, 4, 30);
        gbc.anchor = GridBagConstraints.WEST;
        for (String line : lines) {
            JLabel l = new JLabel(line);
            l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            l.setForeground(new Color(180, 210, 255));
            gbc.gridy++;
            panel.add(l, gbc);
        }

        // Footer
        JLabel footer = new JLabel("v1.0  ·  Final Year Project 2024");
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.setForeground(new Color(120, 160, 210));
        gbc.gridy++;
        gbc.insets = new Insets(30, 30, 0, 30);
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(footer, gbc);

        return panel;
    }

    // ---- Right login-form panel -------------------------------------

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.BG_MAIN);

        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(40, 50, 40, 50)
        ));
        card.setLayout(new BorderLayout(0, 24));
        card.setPreferredSize(new Dimension(430, 480));

        // Title
        JLabel title = new JLabel("Welcome Back");
        title.setFont(UITheme.FONT_HEADING);
        title.setForeground(UITheme.PRIMARY);
        JLabel subtitle = new JLabel("Sign in to your account");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(Color.WHITE);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(subtitle);
        card.add(titlePanel, BorderLayout.NORTH);

        // Tabs: Teacher / Student
        tabPane = new JTabbedPane(JTabbedPane.TOP);
        tabPane.setFont(UITheme.FONT_LABEL);
        tabPane.setBackground(Color.WHITE);
        tabPane.add("Teacher", buildTeacherForm());
        tabPane.add("Student", buildStudentForm());
        card.add(tabPane, BorderLayout.CENTER);

        panel.add(card, new GridBagConstraints());
        return panel;
    }

    // ---- Teacher form -----------------------------------------------

    private JPanel buildTeacherForm() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setLayout(new GridBagLayout());
        GridBagConstraints g = formGbc();

        // Employee ID
        g.gridy = 0;
        p.add(formLabel("Employee ID / Email"), g);
        g.gridy = 1;
        teacherIdField = UITheme.styledTextField("e.g. EMP001");
        p.add(teacherIdField, g);

        // Password
        g.gridy = 2;
        p.add(formLabel("Password"), g);
        g.gridy = 3;
        teacherPassField = UITheme.styledPasswordField("Enter password");
        p.add(teacherPassField, g);

        // Message label
        g.gridy = 4;
        teacherMsgLabel = new JLabel(" ");
        teacherMsgLabel.setFont(UITheme.FONT_SMALL);
        teacherMsgLabel.setForeground(UITheme.DANGER);
        p.add(teacherMsgLabel, g);

        // Login button
        g.gridy = 5;
        teacherLoginBtn = UITheme.primaryButton("Login as Teacher");
        teacherLoginBtn.setPreferredSize(new Dimension(280, 44));
        teacherLoginBtn.addActionListener(e -> doTeacherLogin());
        p.add(teacherLoginBtn, g);

        // Hint
        g.gridy = 6;
        JLabel hint = new JLabel("Default: EMP001 / Teacher@123");
        hint.setFont(UITheme.FONT_SMALL);
        hint.setForeground(UITheme.TEXT_MUTED);
        p.add(hint, g);

        // Enter key support
        teacherPassField.addActionListener(e -> doTeacherLogin());

        return p;
    }

    // ---- Student form -----------------------------------------------

    private JPanel buildStudentForm() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setLayout(new GridBagLayout());
        GridBagConstraints g = formGbc();

        g.gridy = 0;
        p.add(formLabel("Enrollment No / Email"), g);
        g.gridy = 1;
        studentIdField = UITheme.styledTextField("e.g. 2021CSE001");
        p.add(studentIdField, g);

        g.gridy = 2;
        p.add(formLabel("Password"), g);
        g.gridy = 3;
        studentPassField = UITheme.styledPasswordField("Enter password");
        p.add(studentPassField, g);

        g.gridy = 4;
        studentMsgLabel = new JLabel(" ");
        studentMsgLabel.setFont(UITheme.FONT_SMALL);
        studentMsgLabel.setForeground(UITheme.DANGER);
        p.add(studentMsgLabel, g);

        g.gridy = 5;
        studentLoginBtn = UITheme.primaryButton("Login as Student");
        studentLoginBtn.setPreferredSize(new Dimension(280, 44));
        studentLoginBtn.addActionListener(e -> doStudentLogin());
        p.add(studentLoginBtn, g);

        g.gridy = 6;
        JLabel hint = new JLabel("Default: 2021CSE001 / Student@123");
        hint.setFont(UITheme.FONT_SMALL);
        hint.setForeground(UITheme.TEXT_MUTED);
        p.add(hint, g);

        studentPassField.addActionListener(e -> doStudentLogin());

        return p;
    }

    // ---- Login logic ------------------------------------------------

    private void doTeacherLogin() {
        String id   = teacherIdField.getText().trim();
        String pass = new String(teacherPassField.getPassword());

        teacherLoginBtn.setEnabled(false);
        teacherLoginBtn.setText("Logging in...");
        teacherMsgLabel.setText(" ");

        SwingWorker<AuthService.LoginResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AuthService.LoginResult doInBackground() {
                return authService.loginTeacher(id, pass);
            }
            @Override
            protected void done() {
                try {
                    AuthService.LoginResult result = get();
                    if (result.isSuccess()) {
                        Teacher teacher = result.getTeacher();
                        dispose();
                        SwingUtilities.invokeLater(() ->
                            new TeacherDashboardView(teacher));
                    } else {
                        teacherMsgLabel.setText(result.getMessage());
                        teacherPassField.setText("");
                    }
                } catch (Exception ex) {
                    teacherMsgLabel.setText("Error: " + ex.getMessage());
                } finally {
                    teacherLoginBtn.setEnabled(true);
                    teacherLoginBtn.setText("Login as Teacher");
                }
            }
        };
        worker.execute();
    }

    private void doStudentLogin() {
        String id   = studentIdField.getText().trim();
        String pass = new String(studentPassField.getPassword());

        studentLoginBtn.setEnabled(false);
        studentLoginBtn.setText("Logging in...");
        studentMsgLabel.setText(" ");

        SwingWorker<AuthService.LoginResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AuthService.LoginResult doInBackground() {
                return authService.loginStudent(id, pass);
            }
            @Override
            protected void done() {
                try {
                    AuthService.LoginResult result = get();
                    if (result.isSuccess()) {
                        Student student = result.getStudent();
                        dispose();
                        SwingUtilities.invokeLater(() ->
                            new StudentDashboardView(student));
                    } else {
                        studentMsgLabel.setText(result.getMessage());
                        studentPassField.setText("");
                    }
                } catch (Exception ex) {
                    studentMsgLabel.setText("Error: " + ex.getMessage());
                } finally {
                    studentLoginBtn.setEnabled(true);
                    studentLoginBtn.setText("Login as Student");
                }
            }
        };
        worker.execute();
    }

    // ---- Helpers ----------------------------------------------------

    private GridBagConstraints formGbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.fill  = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.insets = new Insets(4, 0, 4, 0);
        return g;
    }

    private JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_LABEL);
        l.setForeground(UITheme.TEXT_PRIMARY);
        return l;
    }

    // ---- QR icon drawn with Swing -----------------------------------

    private static class QRIconPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int margin = 8;
            int cell   = (w - margin * 2) / 7;

            // QR border
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Float(margin/2f, margin/2f,
                    w - margin, h - margin, 10, 10));

            // Draw simple QR-like pattern
            int[][] pattern = {
                {1,1,1,0,1,1,1},
                {1,0,1,0,1,0,1},
                {1,1,1,0,1,1,1},
                {0,0,0,0,0,1,0},
                {1,1,1,0,1,0,1},
                {1,0,1,0,0,1,1},
                {1,1,1,0,1,0,1}
            };
            for (int row = 0; row < 7; row++) {
                for (int col = 0; col < 7; col++) {
                    if (pattern[row][col] == 1) {
                        g2.setColor(Color.WHITE);
                        g2.fillRoundRect(margin + col * cell, margin + row * cell,
                                cell - 1, cell - 1, 3, 3);
                    }
                }
            }
            g2.dispose();
        }
    }
}
