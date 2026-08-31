package com.attendance.view;

import com.attendance.service.AttendanceService;
import com.attendance.service.QRCodeService;
import com.attendance.model.Student;
import com.attendance.utils.UITheme;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.LuminanceSource;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Live webcam QR scanner panel.
 * Continuously reads webcam frames and decodes any QR code found.
 */
public class ScanQRPanel extends JPanel {

    private final Student            student;
    private final AttendanceService  attendanceService = new AttendanceService();
    private final QRCodeService      qrCodeService     = new QRCodeService();

    // Webcam
    private Webcam                   webcam;
    private WebcamPanel              webcamPanel;
    private ScheduledExecutorService scanner;
    private ScheduledFuture<?>       scanTask;
    private volatile boolean         scanning          = false;
    private volatile boolean         processing        = false; // prevent double-submit

    // UI
    private JLabel      statusLabel;
    private JLabel      resultLabel;
    private JProgressBar progressBar;
    private JButton     startBtn;
    private JButton     stopBtn;
    private JPanel      cameraHolder;

    public ScanQRPanel(Student student) {
        this.student = student;
        setBackground(UITheme.BG_MAIN);
        setLayout(new BorderLayout());
        buildUI();
    }

    // ---- UI construction -------------------------------------------

    private void buildUI() {
        JPanel outer = new JPanel(new BorderLayout(0, 16));
        outer.setBackground(UITheme.BG_MAIN);
        outer.setBorder(new EmptyBorder(24, 24, 24, 24));

        // Heading
        JLabel heading = new JLabel("Scan QR Code — Live Camera");
        heading.setFont(UITheme.FONT_HEADING);
        heading.setForeground(UITheme.PRIMARY);
        outer.add(heading, BorderLayout.NORTH);

        // Centre: camera view + controls side by side
        JPanel centre = new JPanel(new BorderLayout(16, 0));
        centre.setBackground(UITheme.BG_MAIN);

        // Camera card (left)
        JPanel camCard = UITheme.cardPanel();
        camCard.setLayout(new BorderLayout());
        camCard.setPreferredSize(new Dimension(480, 380));

        cameraHolder = new JPanel(new BorderLayout());
        cameraHolder.setBackground(Color.BLACK);
        cameraHolder.setPreferredSize(new Dimension(480, 360));

        JLabel placeholder = new JLabel(
            "<html><center><font color='white' size='5'>📷</font><br><br>" +
            "<font color='#aaaaaa'>Camera preview will appear here.<br>Click <b>Start Camera</b> to begin.</font></center></html>",
            SwingConstants.CENTER);
        cameraHolder.add(placeholder, BorderLayout.CENTER);
        camCard.add(cameraHolder, BorderLayout.CENTER);
        centre.add(camCard, BorderLayout.CENTER);

        // Controls card (right)
        JPanel controlCard = UITheme.cardPanel();
        controlCard.setLayout(new BoxLayout(controlCard, BoxLayout.Y_AXIS));
        controlCard.setPreferredSize(new Dimension(230, 380));

        controlCard.add(Box.createVerticalStrut(16));

        JLabel howTo = new JLabel("<html><b>How to use:</b></html>");
        howTo.setFont(UITheme.FONT_LABEL);
        howTo.setForeground(UITheme.PRIMARY);
        howTo.setAlignmentX(CENTER_ALIGNMENT);
        controlCard.add(howTo);
        controlCard.add(Box.createVerticalStrut(10));

        String[] steps = {
            "1. Click Start Camera",
            "2. Point camera at the",
            "   QR code on screen",
            "3. Hold steady for 1–2s",
            "4. Attendance marked!"
        };
        for (String s : steps) {
            JLabel l = new JLabel(s);
            l.setFont(UITheme.FONT_SMALL);
            l.setForeground(UITheme.TEXT_SECONDARY);
            l.setAlignmentX(CENTER_ALIGNMENT);
            controlCard.add(l);
        }

        controlCard.add(Box.createVerticalStrut(24));

        // Start button
        startBtn = UITheme.successButton("▶  Start Camera");
        startBtn.setPreferredSize(new Dimension(190, 46));
        startBtn.setMaximumSize(new Dimension(190, 46));
        startBtn.setAlignmentX(CENTER_ALIGNMENT);
        startBtn.addActionListener(e -> startCamera());
        controlCard.add(startBtn);
        controlCard.add(Box.createVerticalStrut(10));

        // Stop button
        stopBtn = UITheme.dangerButton("⏹  Stop Camera");
        stopBtn.setPreferredSize(new Dimension(190, 46));
        stopBtn.setMaximumSize(new Dimension(190, 46));
        stopBtn.setAlignmentX(CENTER_ALIGNMENT);
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(e -> stopCamera());
        controlCard.add(stopBtn);
        controlCard.add(Box.createVerticalStrut(24));

        // Divider
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(190, 1));
        sep.setAlignmentX(CENTER_ALIGNMENT);
        controlCard.add(sep);
        controlCard.add(Box.createVerticalStrut(16));

        // Or use image file
        JLabel orLabel = new JLabel("— or use image file —");
        orLabel.setFont(UITheme.FONT_SMALL);
        orLabel.setForeground(UITheme.TEXT_MUTED);
        orLabel.setAlignmentX(CENTER_ALIGNMENT);
        controlCard.add(orLabel);
        controlCard.add(Box.createVerticalStrut(10));

        JButton fileBtn = UITheme.outlineButton("📂  Select Image");
        fileBtn.setPreferredSize(new Dimension(190, 40));
        fileBtn.setMaximumSize(new Dimension(190, 40));
        fileBtn.setAlignmentX(CENTER_ALIGNMENT);
        fileBtn.addActionListener(e -> scanFromFile());
        controlCard.add(fileBtn);

        controlCard.add(Box.createVerticalGlue());
        centre.add(controlCard, BorderLayout.EAST);
        outer.add(centre, BorderLayout.CENTER);

        // Status bar (bottom)
        JPanel statusCard = UITheme.cardPanel();
        statusCard.setLayout(new BoxLayout(statusCard, BoxLayout.Y_AXIS));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        progressBar.setAlignmentX(LEFT_ALIGNMENT);
        statusCard.add(progressBar);
        statusCard.add(Box.createVerticalStrut(8));

        statusLabel = new JLabel("Click 'Start Camera' to begin scanning.");
        statusLabel.setFont(UITheme.FONT_BODY);
        statusLabel.setForeground(UITheme.TEXT_SECONDARY);
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);
        statusCard.add(statusLabel);

        resultLabel = new JLabel(" ");
        resultLabel.setFont(UITheme.FONT_SUBHEADING);
        resultLabel.setForeground(UITheme.SUCCESS);
        resultLabel.setAlignmentX(LEFT_ALIGNMENT);
        statusCard.add(resultLabel);

        outer.add(statusCard, BorderLayout.SOUTH);
        add(outer, BorderLayout.CENTER);
    }

    // ---- Camera control --------------------------------------------

    private void startCamera() {
        setStatus("Starting camera...", UITheme.TEXT_SECONDARY);
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        startBtn.setEnabled(false);

        SwingWorker<Webcam, Void> w = new SwingWorker<>() {
            @Override
            protected Webcam doInBackground() throws Exception {
                Webcam cam = Webcam.getDefault();
                if (cam == null) throw new Exception("No webcam detected.");
                cam.setViewSize(WebcamResolution.VGA.getSize());
                cam.open();
                return cam;
            }

            @Override
            protected void done() {
                try {
                    webcam = get();
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);

                    // Embed WebcamPanel
                    webcamPanel = new WebcamPanel(webcam);
                    webcamPanel.setFPSDisplayed(false);
                    webcamPanel.setDisplayDebugInfo(false);
                    webcamPanel.setImageSizeDisplayed(false);
                    webcamPanel.setMirrored(true);

                    cameraHolder.removeAll();
                    cameraHolder.add(webcamPanel, BorderLayout.CENTER);
                    cameraHolder.revalidate();
                    cameraHolder.repaint();

                    startBtn.setEnabled(false);
                    stopBtn.setEnabled(true);
                    scanning = true;
                    processing = false;
                    setStatus("📷 Camera active — point at QR code", UITheme.SUCCESS);

                    // Start scan loop every 300ms
                    scanner = Executors.newSingleThreadScheduledExecutor();
                    scanTask = scanner.scheduleAtFixedRate(
                        ScanQRPanel.this::tryScanFrame, 500, 300, TimeUnit.MILLISECONDS);

                } catch (Exception ex) {
                    progressBar.setVisible(false);
                    startBtn.setEnabled(true);
                    setStatus("❌ " + ex.getMessage(), UITheme.DANGER);
                }
            }
        };
        w.execute();
    }

    private void stopCamera() {
        scanning = false;
        if (scanTask  != null) scanTask.cancel(true);
        if (scanner   != null) scanner.shutdownNow();
        if (webcamPanel != null) webcamPanel.stop();
        if (webcam    != null && webcam.isOpen()) webcam.close();

        cameraHolder.removeAll();
        JLabel placeholder = new JLabel(
            "<html><center><font color='white' size='5'>📷</font><br><br>" +
            "<font color='#aaaaaa'>Camera stopped.<br>Click <b>Start Camera</b> to scan again.</font></center></html>",
            SwingConstants.CENTER);
        cameraHolder.add(placeholder, BorderLayout.CENTER);
        cameraHolder.revalidate();
        cameraHolder.repaint();

        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        setStatus("Camera stopped.", UITheme.TEXT_SECONDARY);
    }

    // ---- QR decode loop --------------------------------------------

    private void tryScanFrame() {
        if (!scanning || processing || webcam == null || !webcam.isOpen()) return;
        try {
            BufferedImage image = webcam.getImage();
            if (image == null) return;

            String decoded = decodeQR(image);
            if (decoded != null) {
                processing = true; // block further scans while we submit
                scanning   = false;
                if (scanTask != null) scanTask.cancel(false);
                SwingUtilities.invokeLater(() -> submitAttendance(decoded));
            }
        } catch (Exception ignored) {}
    }

    private String decodeQR(BufferedImage image) {
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap   bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result         result = new MultiFormatReader().decode(bitmap);
            return result.getText();
        } catch (NotFoundException e) {
            return null; // no QR in frame — normal
        }
    }

    // ---- Attendance submission -------------------------------------

    private void submitAttendance(String payload) {
        stopCamera();
        setStatus("⏳ QR detected! Submitting attendance...", UITheme.ACCENT);
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        resultLabel.setText(" ");

        SwingWorker<AttendanceService.MarkResult, Void> w = new SwingWorker<>() {
            @Override
            protected AttendanceService.MarkResult doInBackground() {
                return attendanceService.markAttendanceByQR(student.getStudentId(), payload);
            }

            @Override
            protected void done() {
                try {
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                    AttendanceService.MarkResult result = get();
                    if (result.isSuccess()) {
                        setStatus("✅ " + result.getMessage(), UITheme.SUCCESS);
                        resultLabel.setText("Attendance Recorded Successfully!");
                        resultLabel.setForeground(UITheme.SUCCESS);
                        // Flash green feedback
                        flashSuccess();
                    } else {
                        setStatus("⚠ " + result.getMessage(), UITheme.DANGER);
                        resultLabel.setText(" ");
                        processing = false; // allow retry
                    }
                } catch (Exception ex) {
                    progressBar.setVisible(false);
                    setStatus("Error: " + ex.getMessage(), UITheme.DANGER);
                    processing = false;
                }
            }
        };
        w.execute();
    }

    // ---- File fallback --------------------------------------------

    private void scanFromFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select QR Code Image");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Images (*.png *.jpg *.jpeg)", "png", "jpg", "jpeg"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        setStatus("Decoding image...", UITheme.TEXT_SECONDARY);

        File file = fc.getSelectedFile();
        SwingWorker<String, Void> w = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                BufferedImage img = javax.imageio.ImageIO.read(file);
                return decodeQR(img);
            }

            @Override
            protected void done() {
                try {
                    String payload = get();
                    progressBar.setIndeterminate(false);
                    if (payload == null) {
                        progressBar.setVisible(false);
                        setStatus("❌ No QR code found in image.", UITheme.DANGER);
                    } else {
                        submitAttendance(payload);
                    }
                } catch (Exception ex) {
                    progressBar.setVisible(false);
                    setStatus("Error: " + ex.getMessage(), UITheme.DANGER);
                }
            }
        };
        w.execute();
    }

    // ---- Helpers --------------------------------------------------

    private void setStatus(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(msg);
            statusLabel.setForeground(color);
        });
    }

    private void flashSuccess() {
        // Brief green flash on the camera holder
        Timer t = new Timer(100, null);
        final int[] count = {0};
        t.addActionListener(e -> {
            cameraHolder.setBackground(count[0] % 2 == 0 ? new Color(39, 174, 96) : Color.BLACK);
            if (++count[0] >= 6) ((Timer) e.getSource()).stop();
        });
        t.start();
    }

    /** Called when this panel is hidden — always release camera */
    public void onHide() {
        if (scanning) stopCamera();
    }
}
