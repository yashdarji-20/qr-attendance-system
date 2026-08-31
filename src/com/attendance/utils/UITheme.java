package com.attendance.utils;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Central theme configuration for the Attendance System UI.
 * Blue + White professional theme.
 */
public final class UITheme {

    // ---- Brand Colours -----------------------------------------------
    public static final Color PRIMARY        = new Color(0x1A3C6E);   // deep navy blue
    public static final Color PRIMARY_LIGHT  = new Color(0x2A5298);   // medium blue
    public static final Color PRIMARY_DARK   = new Color(0x0D1F3C);   // very dark blue
    public static final Color ACCENT         = new Color(0x3498DB);   // sky blue
    public static final Color ACCENT_HOVER   = new Color(0x2980B9);   // darker sky
    public static final Color SUCCESS        = new Color(0x27AE60);   // green
    public static final Color WARNING        = new Color(0xF39C12);   // orange
    public static final Color DANGER         = new Color(0xE74C3C);   // red
    public static final Color INFO           = new Color(0x3498DB);   // blue

    // ---- Background / Surface ----------------------------------------
    public static final Color BG_MAIN        = new Color(0xF0F4F8);   // light grey-blue
    public static final Color BG_SIDEBAR     = new Color(0x1A3C6E);   // same as PRIMARY
    public static final Color BG_CARD        = Color.WHITE;
    public static final Color BG_TABLE_HEAD  = new Color(0x1A3C6E);
    public static final Color BG_TABLE_ALT   = new Color(0xEBF4FD);

    // ---- Text Colours ------------------------------------------------
    public static final Color TEXT_PRIMARY   = new Color(0x1A1A2E);
    public static final Color TEXT_SECONDARY = new Color(0x555577);
    public static final Color TEXT_LIGHT     = Color.WHITE;
    public static final Color TEXT_MUTED     = new Color(0x9E9EA7);
    public static final Color TEXT_LINK      = new Color(0x2A5298);

    // ---- Borders -----------------------------------------------------
    public static final Color BORDER_COLOR   = new Color(0xD5E3F0);
    public static final Color SEPARATOR      = new Color(0xE2EAF4);

    // ---- Fonts -------------------------------------------------------
    public static Font FONT_TITLE;
    public static Font FONT_HEADING;
    public static Font FONT_SUBHEADING;
    public static Font FONT_BODY;
    public static Font FONT_SMALL;
    public static Font FONT_BUTTON;
    public static Font FONT_TABLE_HEADER;
    public static Font FONT_LABEL;
    public static Font FONT_MONO;

    // ---- Constants ---------------------------------------------------
    public static final int  CORNER_RADIUS   = 12;
    public static final int  CARD_PADDING    = 20;
    public static final int  SIDEBAR_WIDTH   = 220;
    public static final int  TOPBAR_HEIGHT   = 60;

    static {
        try {
            // Try to load Inter font; fall back to system sans-serif
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Font base = new Font("Segoe UI", Font.PLAIN, 14);
            if (base.getFamily().equalsIgnoreCase("Dialog")) {
                base = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
            }
            FONT_TITLE        = base.deriveFont(Font.BOLD,  26f);
            FONT_HEADING      = base.deriveFont(Font.BOLD,  20f);
            FONT_SUBHEADING   = base.deriveFont(Font.BOLD,  16f);
            FONT_BODY         = base.deriveFont(Font.PLAIN, 14f);
            FONT_SMALL        = base.deriveFont(Font.PLAIN, 12f);
            FONT_BUTTON       = base.deriveFont(Font.BOLD,  14f);
            FONT_TABLE_HEADER = base.deriveFont(Font.BOLD,  13f);
            FONT_LABEL        = base.deriveFont(Font.BOLD,  13f);
            FONT_MONO         = new Font(Font.MONOSPACED, Font.PLAIN, 13);
        } catch (Exception e) {
            FONT_TITLE        = new Font(Font.SANS_SERIF, Font.BOLD,  26);
            FONT_HEADING      = new Font(Font.SANS_SERIF, Font.BOLD,  20);
            FONT_SUBHEADING   = new Font(Font.SANS_SERIF, Font.BOLD,  16);
            FONT_BODY         = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
            FONT_SMALL        = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
            FONT_BUTTON       = new Font(Font.SANS_SERIF, Font.BOLD,  14);
            FONT_TABLE_HEADER = new Font(Font.SANS_SERIF, Font.BOLD,  13);
            FONT_LABEL        = new Font(Font.SANS_SERIF, Font.BOLD,  13);
            FONT_MONO         = new Font(Font.MONOSPACED, Font.PLAIN, 13);
        }
    }

    private UITheme() {}

    // ---- Factory Methods --------------------------------------------

    /** Create a rounded primary button */
    public static RoundedButton primaryButton(String text) {
        return new RoundedButton(text, PRIMARY, TEXT_LIGHT);
    }

    /** Create a rounded accent button */
    public static RoundedButton accentButton(String text) {
        return new RoundedButton(text, ACCENT, TEXT_LIGHT);
    }

    /** Create a rounded success/green button */
    public static RoundedButton successButton(String text) {
        return new RoundedButton(text, SUCCESS, TEXT_LIGHT);
    }

    /** Create a rounded danger/red button */
    public static RoundedButton dangerButton(String text) {
        return new RoundedButton(text, DANGER, TEXT_LIGHT);
    }

    /** Create a rounded warning/orange button */
    public static RoundedButton warningButton(String text) {
        return new RoundedButton(text, WARNING, TEXT_LIGHT);
    }

    /** Create a rounded outline button */
    public static RoundedButton outlineButton(String text) {
        return new RoundedButton(text, Color.WHITE, PRIMARY);
    }

    /** Styled text field */
    public static JTextField styledTextField(String placeholder) {
        JTextField field = new JTextField();
        styleTextField(field, placeholder);
        return field;
    }

    /** Styled password field */
    public static JPasswordField styledPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        styleTextField(field, placeholder);
        return field;
    }

    private static void styleTextField(JTextField field, String placeholder) {
        field.setFont(FONT_BODY);
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setPreferredSize(new Dimension(250, 42));
        if (placeholder != null && !placeholder.isBlank()) {
            field.putClientProperty("JTextField.placeholderText", placeholder);
        }
    }

    /** Styled label */
    public static JLabel label(String text, Font font, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(color);
        return lbl;
    }

    /** Card panel with shadow-like border */
    public static JPanel cardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(CARD_PADDING, CARD_PADDING,
                                            CARD_PADDING, CARD_PADDING)
        ));
        return panel;
    }

    /** Apply uniform table styling */
    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(36);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(0xBDD9F2));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.getTableHeader().setFont(FONT_TABLE_HEADER);
        table.getTableHeader().setBackground(BG_TABLE_HEAD);
        table.getTableHeader().setForeground(TEXT_LIGHT);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
    }

    /** Style a JComboBox */
    public static void styleComboBox(JComboBox<?> combo) {
        combo.setFont(FONT_BODY);
        combo.setBackground(Color.WHITE);
        combo.setForeground(TEXT_PRIMARY);
        combo.setPreferredSize(new Dimension(200, 38));
        combo.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
    }

    // ---- Inner Classes -----------------------------------------------

    /**
     * Custom rounded-corner button with hover effect.
     */
    public static class RoundedButton extends JButton {
        private final Color normalBg;
        private final Color normalFg;
        private Color currentBg;

        public RoundedButton(String text, Color bg, Color fg) {
            super(text);
            this.normalBg  = bg;
            this.normalFg  = fg;
            this.currentBg = bg;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setFont(FONT_BUTTON);
            setForeground(fg);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(160, 42));

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    currentBg = normalBg.darker();
                    repaint();
                }
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    currentBg = normalBg;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(currentBg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS));
            g2.dispose();
            super.paintComponent(g);
        }

        /** Set background to distinguish active/inactive state */
        public void setActive(boolean active) {
            currentBg = active ? normalBg : new Color(0xB0C4DE);
            repaint();
        }
    }
}
