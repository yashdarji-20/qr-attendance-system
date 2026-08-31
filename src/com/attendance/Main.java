package com.attendance;

import com.attendance.database.DatabaseConnection;
import com.attendance.view.LoginView;
import com.formdev.flatlaf.FlatLightLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Application entry point.
 * Sets up Look & Feel, tests DB connectivity, then launches the Login screen.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("QR Attendance System starting...");

        // 1. Setup Look & Feel (FlatLaf for modern UI)
        setupLookAndFeel();

        // 2. Test database connection
        if (!DatabaseConnection.getInstance().testConnection()) {
            showDBError();
            System.exit(1);
        }

        log.info("Database connected. Launching UI...");

        // 3. Launch Login Screen on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                new LoginView();
                log.info("Application launched successfully.");
            } catch (Exception e) {
                log.error("Failed to launch application: {}", e.getMessage(), e);
                JOptionPane.showMessageDialog(null,
                    "Failed to start: " + e.getMessage(),
                    "Startup Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });

        // 4. Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            DatabaseConnection.getInstance().shutdown();
        }));
    }

    private static void setupLookAndFeel() {
        try {
            // Try FlatLaf modern look
            FlatLightLaf.setup();
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 6);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
            log.info("FlatLaf Look & Feel applied.");
        } catch (Exception e) {
            log.warn("FlatLaf not available, using system L&F: {}", e.getMessage());
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                log.warn("System L&F failed, using default.");
            }
        }
    }

    private static void showDBError() {
        SwingUtilities.invokeLater(() -> {
            JPanel panel = new JPanel(new GridLayout(5, 1, 6, 6));
            panel.add(new JLabel("Could not connect to the MySQL database."));
            panel.add(new JLabel("Please check the following:"));
            panel.add(new JLabel("  1. MySQL server is running"));
            panel.add(new JLabel("  2. Database 'attendance_system' exists"));
            panel.add(new JLabel("  3. Credentials in resources/db.properties are correct"));
            JOptionPane.showMessageDialog(null, panel,
                "Database Connection Error", JOptionPane.ERROR_MESSAGE);
        });
    }
}
