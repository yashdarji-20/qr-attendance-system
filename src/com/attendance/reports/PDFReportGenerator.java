package com.attendance.reports;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates professional PDF attendance reports using iText 7.
 */
public class PDFReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(PDFReportGenerator.class);

    // Theme colors
    private static final DeviceRgb PRIMARY      = new DeviceRgb(26, 60, 110);
    private static final DeviceRgb ACCENT       = new DeviceRgb(52, 152, 219);
    private static final DeviceRgb SUCCESS      = new DeviceRgb(39, 174, 96);
    private static final DeviceRgb DANGER       = new DeviceRgb(231, 76, 60);
    private static final DeviceRgb HEADER_BG    = new DeviceRgb(26, 60, 110);
    private static final DeviceRgb ALT_ROW_BG   = new DeviceRgb(235, 244, 253);
    private static final DeviceRgb WHITE        = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb LIGHT_GREY   = new DeviceRgb(245, 247, 250);

    /**
     * Generate an attendance percentage report PDF.
     *
     * @param data          rows: [studentId, enrollmentNo, studentName, total, attended, percentage]
     * @param filePath      absolute path for output file
     * @param subjectName   subject name for report header
     * @param className     class name for report header
     * @param teacherName   teacher name for report header
     */
    public static void generateAttendanceReport(List<Object[]> data,
                                                 String filePath,
                                                 String subjectName,
                                                 String className,
                                                 String teacherName) throws Exception {
        log.info("Generating PDF attendance report: {}", filePath);

        try (PdfDocument pdf = new PdfDocument(new PdfWriter(filePath));
             Document doc = new Document(pdf, PageSize.A4)) {

            doc.setMargins(36, 36, 36, 36);

            PdfFont bold  = PdfFontFactory.createFont();
            PdfFont plain = PdfFontFactory.createFont();

            // ---- Header ----
            addHeader(doc, subjectName, className, teacherName);

            // ---- Summary Stats ----
            if (!data.isEmpty()) {
                long below75 = data.stream().filter(r -> (Double) r[5] < 75).count();
                long above75 = data.size() - below75;
                double avgPct = data.stream().mapToDouble(r -> (Double) r[5]).average().orElse(0);
                addSummaryStats(doc, data.size(), above75, below75, avgPct);
            }

            // ---- Attendance Table ----
            Table table = new Table(UnitValue.createPercentArray(
                    new float[]{2.5f, 3.5f, 1.5f, 1.5f, 2f, 2f}))
                    .useAllAvailableWidth();

            // Header row
            String[] headers = {"Enroll No", "Student Name", "Total", "Attended", "Percentage", "Status"};
            for (String h : headers) {
                Cell cell = new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(10).setFontColor(WHITE))
                    .setBackgroundColor(HEADER_BG)
                    .setPadding(8)
                    .setTextAlignment(TextAlignment.CENTER);
                table.addHeaderCell(cell);
            }

            // Data rows
            for (int i = 0; i < data.size(); i++) {
                Object[] row  = data.get(i);
                boolean alt   = i % 2 == 1;
                DeviceRgb bg  = alt ? ALT_ROW_BG : WHITE;
                double pct    = (Double) row[5];
                String status = pct >= 75 ? "PASS" : "LOW";
                DeviceRgb statusColor = pct >= 75 ? SUCCESS : DANGER;

                String[] vals = {
                    (String) row[1],
                    (String) row[2],
                    String.valueOf(row[3]),
                    String.valueOf(row[4]),
                    String.format("%.1f%%", pct),
                    status
                };

                for (int j = 0; j < vals.length; j++) {
                    Cell cell = new Cell().add(new Paragraph(vals[j]).setFontSize(9))
                        .setBackgroundColor(bg)
                        .setPadding(6)
                        .setTextAlignment(j < 2 ? TextAlignment.LEFT : TextAlignment.CENTER);
                    if (j == 5) cell.setFontColor(statusColor);
                    table.addCell(cell);
                }
            }

            doc.add(table);

            // ---- Footer ----
            addFooter(doc, teacherName);
        }

        log.info("PDF generated successfully: {}", filePath);
    }

    private static void addHeader(Document doc, String subject, String className, String teacher) {
        // Institution name
        doc.add(new Paragraph("DY PATIL SCHOOL OF BIOTECHNOLOGY AND BIOINFORMATICS")
            .setFontSize(18).setFontColor(PRIMARY)
            .setBold().setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(4));

        doc.add(new Paragraph("Attendance Management System")
            .setFontSize(11).setFontColor(ACCENT)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20));

        // Report title box
        Table titleBox = new Table(1).useAllAvailableWidth();
        Cell titleCell = new Cell()
            .add(new Paragraph("ATTENDANCE REPORT")
                .setFontSize(14).setBold().setFontColor(WHITE)
                .setTextAlignment(TextAlignment.CENTER))
            .setBackgroundColor(PRIMARY).setPadding(12);
        titleBox.addCell(titleCell);
        doc.add(titleBox);
        doc.add(new Paragraph("").setMarginBottom(12));

        // Report info table
        Table info = new Table(UnitValue.createPercentArray(new float[]{3f, 3f}))
                .useAllAvailableWidth().setMarginBottom(16);

        addInfoCell(info, "Subject:", subject);
        addInfoCell(info, "Generated:", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
        addInfoCell(info, "Class:", className);
        addInfoCell(info, "Teacher:", teacher);
        doc.add(info);
    }

    private static void addInfoCell(Table table, String label, String value) {
        Cell cell = new Cell().add(
            new Paragraph().add(new Text(label).setBold()).add(" " + value)
                .setFontSize(10))
            .setPadding(6).setBorder(null);
        table.addCell(cell);
    }

    private static void addSummaryStats(Document doc, int total,
                                         long pass, long fail, double avg) {
        Table stats = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                .useAllAvailableWidth().setMarginBottom(16);

        addStatCell(stats, String.valueOf(total), "Total Students", PRIMARY);
        addStatCell(stats, String.valueOf(pass),  "Passed (≥75%)",  SUCCESS);
        addStatCell(stats, String.valueOf(fail),  "Low (<75%)",     DANGER);
        addStatCell(stats, String.format("%.1f%%", avg), "Class Avg", ACCENT);
        doc.add(stats);
    }

    private static void addStatCell(Table table, String value, String label, DeviceRgb color) {
        Cell cell = new Cell()
            .add(new Paragraph(value).setFontSize(20).setBold().setFontColor(color)
                     .setTextAlignment(TextAlignment.CENTER))
            .add(new Paragraph(label).setFontSize(9).setFontColor(new DeviceRgb(100, 100, 130))
                     .setTextAlignment(TextAlignment.CENTER))
            .setBackgroundColor(LIGHT_GREY).setPadding(12)
            .setBorderRight(new com.itextpdf.layout.borders.SolidBorder(WHITE, 3));
        table.addCell(cell);
    }

    private static void addFooter(Document doc, String teacher) {
        doc.add(new Paragraph("").setMarginTop(20));
        doc.add(new Paragraph(
            "Generated by QR Attendance Management System  •  " + teacher +
            "  •  " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")))
            .setFontSize(8)
            .setFontColor(new DeviceRgb(150, 150, 170))
            .setTextAlignment(TextAlignment.CENTER));
    }
}
