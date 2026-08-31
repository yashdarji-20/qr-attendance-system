package com.attendance.view;

import com.attendance.dao.ClassRoomDAO;
import com.attendance.dao.LectureDAO;
import com.attendance.dao.SubjectDAO;
import com.attendance.model.ClassRoom;
import com.attendance.model.Lecture;
import com.attendance.model.QRToken;
import com.attendance.model.Subject;
import com.attendance.model.Teacher;
import com.attendance.service.QRCodeService;
import com.attendance.utils.UITheme;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Panel where teachers select lecture details and generate a QR code.
 * The QR code auto-expires after 30 seconds with a visual countdown.
 */
public class GenerateQRPanel extends JPanel {

    private final Teacher       teacher;
    private final LectureDAO    lectureDAO;
    private final QRCodeService qrCodeService;
    private SubjectDAO          subjectDAO;
    private ClassRoomDAO        classRoomDAO;

    // Form fields
    private JComboBox<SubjectItem>   subjectCombo;
    private JComboBox<ClassItem>     classCombo;
    private JTextField               topicField;
    private JSpinner                 expirySpinner;

    // QR display
    private QRImagePanel qrPanel;
    private JLabel       tokenLabel;
    private JLabel       countdownLabel;
    private JLabel       statusLabel;
    private JButton      generateBtn;
    private JButton      stopBtn;

    // Timer state
    private Timer    countdownTimer;
    private int      secondsLeft;
    private QRToken  currentToken;

    public GenerateQRPanel(Teacher teacher) {
        this.teacher      = teacher;
        this.lectureDAO   = new LectureDAO();
        this.qrCodeService= new QRCodeService();
        this.subjectDAO   = new SubjectDAO();
        this.classRoomDAO = new ClassRoomDAO();
        setBackground(UITheme.BG_MAIN);
        setLayout(new BorderLayout());
        buildUI();
        loadFormData();
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(24, 0));
        content.setBackground(UITheme.BG_MAIN);
        content.setBorder(new EmptyBorder(24, 24, 24, 24));

        content.add(buildFormPanel(), BorderLayout.WEST);
        content.add(buildQRPanel(),   BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
    }

    // ---- Left: Form -------------------------------------------------

    private JPanel buildFormPanel() {
        JPanel card = UITheme.cardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(320, 0));

        card.add(sectionTitle("Generate QR Code"));
        card.add(Box.createVerticalStrut(20));

        // Subject
        card.add(fieldLabel("Subject *"));
        card.add(Box.createVerticalStrut(4));
        subjectCombo = new JComboBox<>();
        UITheme.styleComboBox(subjectCombo);
        subjectCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        card.add(subjectCombo);
        card.add(Box.createVerticalStrut(14));

        // Class
        card.add(fieldLabel("Class *"));
        card.add(Box.createVerticalStrut(4));
        classCombo = new JComboBox<>();
        UITheme.styleComboBox(classCombo);
        classCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        card.add(classCombo);
        card.add(Box.createVerticalStrut(14));

        // Topic
        card.add(fieldLabel("Topic (optional)"));
        card.add(Box.createVerticalStrut(4));
        topicField = UITheme.styledTextField("e.g. Arrays and Pointers");
        topicField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        card.add(topicField);
        card.add(Box.createVerticalStrut(14));

        // Expiry
        card.add(fieldLabel("QR Expiry (seconds)"));
        card.add(Box.createVerticalStrut(4));
        SpinnerNumberModel spinModel = new SpinnerNumberModel(30, 10, 300, 5);
        expirySpinner = new JSpinner(spinModel);
        expirySpinner.setFont(UITheme.FONT_BODY);
        expirySpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        card.add(expirySpinner);
        card.add(Box.createVerticalStrut(24));

        // Generate button
        generateBtn = UITheme.successButton("🔲  Generate QR Code");
        generateBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        generateBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        generateBtn.addActionListener(e -> generateQR());
        card.add(generateBtn);
        card.add(Box.createVerticalStrut(10));

        // Stop button
        stopBtn = UITheme.dangerButton("⏹  Stop QR");
        stopBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        stopBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(e -> stopQR());
        card.add(stopBtn);
        card.add(Box.createVerticalStrut(20));

        // Info note
        JLabel note = new JLabel("<html><i>The QR code expires automatically.<br>" +
                "Students must scan before the countdown ends.</i></html>");
        note.setFont(UITheme.FONT_SMALL);
        note.setForeground(UITheme.TEXT_MUTED);
        card.add(note);

        return card;
    }

    // ---- Right: QR display ------------------------------------------

    private JPanel buildQRPanel() {
        JPanel card = UITheme.cardPanel();
        card.setLayout(new BorderLayout(0, 16));

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setBackground(Color.WHITE);
        JLabel t = new JLabel("QR Code Display");
        t.setFont(UITheme.FONT_SUBHEADING);
        t.setForeground(UITheme.PRIMARY);
        titleRow.add(t, BorderLayout.WEST);

        // Status
        statusLabel = new JLabel("No QR generated yet");
        statusLabel.setFont(UITheme.FONT_BODY);
        statusLabel.setForeground(UITheme.TEXT_MUTED);
        titleRow.add(statusLabel, BorderLayout.EAST);
        card.add(titleRow, BorderLayout.NORTH);

        // QR Image
        qrPanel = new QRImagePanel();
        qrPanel.setPreferredSize(new Dimension(380, 380));
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(Color.WHITE);
        center.add(qrPanel);
        card.add(center, BorderLayout.CENTER);

        // Countdown + token info
        JPanel footer = new JPanel(new GridLayout(2, 1, 0, 4));
        footer.setBackground(Color.WHITE);

        countdownLabel = new JLabel("——", SwingConstants.CENTER);
        countdownLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        countdownLabel.setForeground(UITheme.PRIMARY);

        tokenLabel = new JLabel("Token will appear here", SwingConstants.CENTER);
        tokenLabel.setFont(UITheme.FONT_MONO);
        tokenLabel.setForeground(UITheme.TEXT_MUTED);

        footer.add(countdownLabel);
        footer.add(tokenLabel);
        card.add(footer, BorderLayout.SOUTH);

        return card;
    }

    // ---- QR Generation logic ----------------------------------------

    private void generateQR() {
        SubjectItem si = (SubjectItem) subjectCombo.getSelectedItem();
        ClassItem   ci = (ClassItem)   classCombo.getSelectedItem();
        if (si == null || ci == null) {
            showError("Please select a subject and class.");
            return;
        }

        // Stop any existing countdown
        stopQR();

        int expiry = (int) expirySpinner.getValue();
        generateBtn.setEnabled(false);
        statusLabel.setText("Generating QR...");
        statusLabel.setForeground(UITheme.INFO);

        SwingWorker<QRCodeService.GenerationResult, Void> worker = new SwingWorker<>() {
            @Override
            protected QRCodeService.GenerationResult doInBackground() throws Exception {
                // Create lecture record
                Lecture lecture = new Lecture();
                lecture.setSubjectId (si.id());
                lecture.setTeacherId (teacher.getTeacherId());
                lecture.setClassId   (ci.id());
                lecture.setLectureDate(Date.valueOf(LocalDate.now()));
                lecture.setStartTime (Time.valueOf(LocalTime.now()));
                lecture.setTopic     (topicField.getText().trim());

                int lectureId = lectureDAO.insert(lecture);
                return qrCodeService.generateQR(lectureId, teacher.getTeacherId(), expiry);
            }

            @Override
            protected void done() {
                try {
                    QRCodeService.GenerationResult result = get();
                    currentToken = result.getQrToken();
                    qrPanel.setImage(result.getQrImage());
                    tokenLabel.setText("Token: " + currentToken.getTokenValue().substring(0, 8) + "...");
                    startCountdown(result.getExpirySeconds());
                    stopBtn.setEnabled(true);
                    statusLabel.setText("✅ QR Active");
                    statusLabel.setForeground(UITheme.SUCCESS);
                } catch (Exception ex) {
                    showError("Failed to generate QR: " + ex.getMessage());
                    generateBtn.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void startCountdown(int seconds) {
        secondsLeft = seconds;
        updateCountdown();

        countdownTimer = new Timer(1000, e -> {
            secondsLeft--;
            updateCountdown();
            if (secondsLeft <= 0) {
                ((Timer) e.getSource()).stop();
                onQRExpired();
            }
        });
        countdownTimer.start();
    }

    private void updateCountdown() {
        countdownLabel.setText(String.valueOf(secondsLeft));
        // Colour: green -> yellow -> red
        if (secondsLeft > 15) countdownLabel.setForeground(UITheme.SUCCESS);
        else if (secondsLeft > 5) countdownLabel.setForeground(UITheme.WARNING);
        else countdownLabel.setForeground(UITheme.DANGER);
    }

    private void onQRExpired() {
        qrPanel.setImage(null);
        countdownLabel.setText("00");
        countdownLabel.setForeground(UITheme.DANGER);
        statusLabel.setText("❌ QR Expired");
        statusLabel.setForeground(UITheme.DANGER);
        tokenLabel.setText("Generate a new QR for next attendance");
        stopBtn.setEnabled(false);
        generateBtn.setEnabled(true);
    }

    private void stopQR() {
        if (countdownTimer != null && countdownTimer.isRunning()) {
            countdownTimer.stop();
        }
        qrPanel.setImage(null);
        countdownLabel.setText("——");
        countdownLabel.setForeground(UITheme.TEXT_MUTED);
        statusLabel.setText("QR stopped by teacher");
        statusLabel.setForeground(UITheme.TEXT_SECONDARY);
        tokenLabel.setText("Token will appear here");
        stopBtn.setEnabled(false);
        generateBtn.setEnabled(true);
    }

    // ---- Data loading -----------------------------------------------

    private void loadFormData() {
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<Subject>   subjects = subjectDAO.findByTeacher(teacher.getTeacherId());
                List<ClassRoom> classes  = classRoomDAO.findByTeacher(teacher.getTeacherId());
                SwingUtilities.invokeLater(() -> {
                    for (Subject s   : subjects) subjectCombo.addItem(new SubjectItem(s));
                    for (ClassRoom c : classes)  classCombo.addItem(new ClassItem(c));
                });
                return null;
            }
        };
        w.execute();
    }

    // ---- Helpers ----------------------------------------------------

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_SUBHEADING);
        l.setForeground(UITheme.PRIMARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_LABEL);
        l.setForeground(UITheme.TEXT_PRIMARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ---- Wrapper classes for combos ---------------------------------

    private record SubjectItem(Subject subject) {
        int id() { return subject.getSubjectId(); }
        @Override public String toString() {
            return subject.getSubjectName() + " [" + subject.getSubjectCode() + "]";
        }
    }

    private record ClassItem(ClassRoom classRoom) {
        int id() { return classRoom.getClassId(); }
        @Override public String toString() { return classRoom.getClassName(); }
    }

    // ---- Custom QR image panel --------------------------------------

    private static class QRImagePanel extends JPanel {
        private BufferedImage image;

        QRImagePanel() {
            setBackground(new Color(0xF8FAFD));
            setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1));
        }

        void setImage(BufferedImage img) {
            this.image = img;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            if (image != null) {
                int padding = 16;
                int w = getWidth()  - padding * 2;
                int h = getHeight() - padding * 2;
                g2.drawImage(image, padding, padding, w, h, this);
            } else {
                // Placeholder
                g2.setColor(UITheme.BORDER_COLOR);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND, 10, new float[]{8, 6}, 0));
                g2.drawRoundRect(30, 30, getWidth()-60, getHeight()-60, 16, 16);
                g2.setFont(UITheme.FONT_BODY);
                g2.setColor(UITheme.TEXT_MUTED);
                String msg = "QR Code will appear here";
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth()  - fm.stringWidth(msg)) / 2;
                int y = (getHeight() + fm.getAscent()) / 2;
                g2.drawString(msg, x, y);
            }
            g2.dispose();
        }
    }
}
