package com.attendance.view;

import com.attendance.dao.DepartmentDAO;
import com.attendance.dao.StudentDAO;
import com.attendance.model.Department;
import com.attendance.model.Student;
import com.attendance.model.Teacher;
import com.attendance.service.AuthService;
import com.attendance.utils.UITheme;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.List;

/**
 * Manage Students panel — add, edit, delete, search students.
 */
public class ManageStudentsPanel extends JPanel {

    private final Teacher     teacher;
    private final StudentDAO  studentDAO    = new StudentDAO();
    private final DepartmentDAO deptDAO     = new DepartmentDAO();

    private JTable  table;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    private static final String[] COLUMNS =
        {"ID", "Enrollment No", "Name", "Email", "Department", "Semester", "Phone"};

    public ManageStudentsPanel(Teacher teacher) {
        this.teacher = teacher;
        setBackground(UITheme.BG_MAIN);
        setLayout(new BorderLayout());
        buildUI();
        loadStudents(null);
    }

    private void buildUI() {
        JPanel outer = new JPanel(new BorderLayout(0, 16));
        outer.setBackground(UITheme.BG_MAIN);
        outer.setBorder(new EmptyBorder(24, 24, 24, 24));

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setOpaque(false);

        JLabel title = new JLabel("Manage Students");
        title.setFont(UITheme.FONT_HEADING);
        title.setForeground(UITheme.PRIMARY);
        topBar.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        // Search
        searchField = UITheme.styledTextField("Search by name, ID, email...");
        searchField.setPreferredSize(new Dimension(260, 38));
        searchField.addActionListener(e -> loadStudents(searchField.getText().trim()));
        JButton searchBtn = UITheme.accentButton("Search");
        searchBtn.setPreferredSize(new Dimension(90, 38));
        searchBtn.addActionListener(e -> loadStudents(searchField.getText().trim()));

        UITheme.RoundedButton addBtn = UITheme.successButton("+ Add Student");
        addBtn.setPreferredSize(new Dimension(130, 38));
        addBtn.addActionListener(e -> showAddDialog(null));

        UITheme.RoundedButton editBtn = UITheme.primaryButton("✏ Edit");
        editBtn.setPreferredSize(new Dimension(90, 38));
        editBtn.addActionListener(e -> showEditSelected());

        UITheme.RoundedButton deleteBtn = UITheme.dangerButton("🗑 Delete");
        deleteBtn.setPreferredSize(new Dimension(100, 38));
        deleteBtn.addActionListener(e -> deleteSelected());

        UITheme.RoundedButton refreshBtn = UITheme.outlineButton("↻ Refresh");
        refreshBtn.setPreferredSize(new Dimension(100, 38));
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            loadStudents(null);
        });

        actions.add(searchField);
        actions.add(searchBtn);
        actions.add(Box.createHorizontalStrut(8));
        actions.add(addBtn);
        actions.add(editBtn);
        actions.add(deleteBtn);
        actions.add(refreshBtn);
        topBar.add(actions, BorderLayout.EAST);
        outer.add(topBar, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : UITheme.BG_TABLE_ALT);
                }
                return c;
            }
        };
        UITheme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Hide ID column (col 0)
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        // Double-click to edit
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) showEditSelected();
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel tableCard = UITheme.cardPanel();
        tableCard.setLayout(new BorderLayout());
        tableCard.add(scroll, BorderLayout.CENTER);
        outer.add(tableCard, BorderLayout.CENTER);

        // Footer row count
        JLabel footerLabel = new JLabel("0 students");
        footerLabel.setFont(UITheme.FONT_SMALL);
        footerLabel.setForeground(UITheme.TEXT_MUTED);
        outer.add(footerLabel, BorderLayout.SOUTH);

        add(outer, BorderLayout.CENTER);
    }

    // ---- Data loading -----------------------------------------------

    void loadStudents(String keyword) {
        SwingWorker<List<Student>, Void> w = new SwingWorker<>() {
            @Override
            protected List<Student> doInBackground() throws Exception {
                if (keyword == null || keyword.isBlank()) {
                    return studentDAO.findAll();
                }
                return studentDAO.search(keyword);
            }
            @Override
            protected void done() {
                try {
                    List<Student> students = get();
                    tableModel.setRowCount(0);
                    for (Student s : students) {
                        tableModel.addRow(new Object[]{
                            s.getStudentId(),
                            s.getEnrollmentNo(),
                            s.getFullName(),
                            s.getEmail(),
                            s.getDepartmentName() != null ? s.getDepartmentName() : "—",
                            "Sem " + s.getSemester(),
                            s.getPhone() != null ? s.getPhone() : "—"
                        });
                    }
                } catch (Exception e) {
                    showError("Failed to load students: " + e.getMessage());
                }
            }
        };
        w.execute();
    }

    // ---- CRUD operations --------------------------------------------

    private void showAddDialog(Student existing) {
        boolean isEdit = existing != null;
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isEdit ? "Edit Student" : "Add New Student", true);
        dialog.setSize(550, 520);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(24, 32, 24, 32));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0; g.insets = new Insets(6, 0, 6, 0);

        // Form fields
        JTextField enrollField = field(existing != null ? existing.getEnrollmentNo() : "");
        JTextField firstField  = field(existing != null ? existing.getFirstName()    : "");
        JTextField lastField   = field(existing != null ? existing.getLastName()     : "");
        JTextField emailField  = field(existing != null ? existing.getEmail()        : "");
        JTextField phoneField  = field(existing != null ? existing.getPhone()        : "");
        JPasswordField passField = new JPasswordField();
        styleDialogField(passField);

        JComboBox<DeptItem> deptCombo = new JComboBox<>();
        UITheme.styleComboBox(deptCombo);
        SpinnerNumberModel semModel = new SpinnerNumberModel(
                existing != null ? existing.getSemester() : 1, 1, 8, 1);
        JSpinner semSpinner = new JSpinner(semModel);

        // Load departments
        try {
            List<Department> depts = deptDAO.findAll();
            for (Department d : depts) {
                DeptItem item = new DeptItem(d);
                deptCombo.addItem(item);
                if (existing != null && d.getDepartmentId() == existing.getDepartmentId()) {
                    deptCombo.setSelectedItem(item);
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }

        int row = 0;
        g.gridy = row++;
        panel.add(formSection("Personal Information"), g);
        g.gridy = row++; panel.add(labeledField("Enrollment No *", enrollField), g);
        g.gridy = row++; panel.add(labeledField("First Name *",    firstField),  g);
        g.gridy = row++; panel.add(labeledField("Last Name *",     lastField),   g);
        g.gridy = row++; panel.add(labeledField("Email *",         emailField),  g);
        g.gridy = row++; panel.add(labeledField("Phone",           phoneField),  g);
        if (!isEdit) {
            g.gridy = row++; panel.add(labeledField("Password *", passField), g);
        }
        g.gridy = row++;
        panel.add(formSection("Academic Information"), g);
        g.gridy = row++; panel.add(labeledField("Department", deptCombo), g);
        g.gridy = row++; panel.add(labeledField("Semester",   semSpinner), g);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setBackground(Color.WHITE);
        UITheme.RoundedButton saveBtn   = UITheme.successButton(isEdit ? "Update" : "Add Student");
        UITheme.RoundedButton cancelBtn = UITheme.dangerButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            // Validation
            if (enrollField.getText().isBlank() || firstField.getText().isBlank() ||
                lastField.getText().isBlank()   || emailField.getText().isBlank()) {
                JOptionPane.showMessageDialog(dialog, "Please fill all required fields (*)",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                DeptItem di = (DeptItem) deptCombo.getSelectedItem();
                if (isEdit) {
                    existing.setFirstName   (firstField.getText().trim());
                    existing.setLastName    (lastField.getText().trim());
                    existing.setEmail       (emailField.getText().trim());
                    existing.setPhone       (phoneField.getText().trim());
                    existing.setDepartmentId(di != null ? di.dept.getDepartmentId() : 0);
                    existing.setSemester    ((int) semSpinner.getValue());
                    studentDAO.update(existing);
                } else {
                    String pass = new String(passField.getPassword());
                    if (pass.isBlank()) {
                        JOptionPane.showMessageDialog(dialog, "Password is required.",
                                "Validation Error", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    Student s = new Student(
                        enrollField.getText().trim(),
                        firstField.getText().trim(),
                        lastField.getText().trim(),
                        emailField.getText().trim(),
                        phoneField.getText().trim(),
                        AuthService.hashPassword(pass),
                        di != null ? di.dept.getDepartmentId() : 0,
                        (int) semSpinner.getValue(), 3
                    );
                    studentDAO.insert(s);
                }
                dialog.dispose();
                loadStudents(null);
                JOptionPane.showMessageDialog(ManageStudentsPanel.this,
                        isEdit ? "Student updated!" : "Student added successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        g.gridy = row++;
        g.insets = new Insets(16, 0, 0, 0);
        panel.add(btnRow, g);

        dialog.setContentPane(new JScrollPane(panel));
        dialog.setVisible(true);
    }

    private void showEditSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { showError("Please select a student to edit."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        try {
            studentDAO.findById(id).ifPresent(s -> showAddDialog(s));
        } catch (Exception ex) {
            showError("Error: " + ex.getMessage());
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { showError("Please select a student to delete."); return; }
        int id   = (int)    tableModel.getValueAt(row, 0);
        String nm = (String) tableModel.getValueAt(row, 2);
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete student: " + nm + "?\nThis action cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            studentDAO.delete(id);
            loadStudents(null);
        } catch (Exception ex) {
            showError("Error deleting: " + ex.getMessage());
        }
    }

    // ---- Helpers ----------------------------------------------------

    private JTextField field(String value) {
        JTextField f = UITheme.styledTextField("");
        f.setText(value);
        return f;
    }

    private void styleDialogField(JTextField f) {
        f.setFont(UITheme.FONT_BODY);
        f.setPreferredSize(new Dimension(250, 38));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
    }

    private JPanel labeledField(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(Color.WHITE);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_LABEL);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        lbl.setPreferredSize(new Dimension(130, 36));
        row.add(lbl,   BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JLabel formSection(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_LABEL);
        l.setForeground(UITheme.PRIMARY);
        l.setBorder(new MatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR));
        return l;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ---- Inner record -----------------------------------------------
    private record DeptItem(Department dept) {
        @Override public String toString() { return dept.getDepartmentName(); }
    }
}
