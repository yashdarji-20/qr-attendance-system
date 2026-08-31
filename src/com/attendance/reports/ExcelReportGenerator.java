package com.attendance.reports;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates styled Excel (.xlsx) attendance reports using Apache POI.
 */
public class ExcelReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(ExcelReportGenerator.class);

    // Brand colour (navy blue) as indexed colour
    private static final byte[] PRIMARY_RGB  = {26, 60, 110};
    private static final byte[] SUCCESS_RGB  = {39, (byte)174, 96};
    private static final byte[] DANGER_RGB   = {(byte)231, 76, 60};
    private static final byte[] ACCENT_RGB   = {52, (byte)152, (byte)219};
    private static final byte[] ALT_ROW_RGB  = {(byte)235, (byte)244, (byte)253};

    /**
     * Generate attendance percentage report.
     *
     * @param data        rows: [studentId, enrollmentNo, studentName, total, attended, percentage]
     * @param filePath    absolute path for .xlsx output
     * @param subjectName subject name
     * @param className   class name
     */
    public static void generateAttendanceReport(List<Object[]> data,
                                                  String filePath,
                                                  String subjectName,
                                                  String className) throws Exception {
        log.info("Generating Excel report: {}", filePath);

        try (XSSFWorkbook wb = new XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(filePath)) {

            XSSFSheet sheet = wb.createSheet("Attendance Report");
            sheet.setDefaultColumnWidth(18);
            sheet.setColumnWidth(1, 26 * 256); // Student Name wider

            // ---- Title rows ----
            int rowNum = 0;
            Row titleRow = sheet.createRow(rowNum++);
            titleRow.setHeightInPoints(32);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("ATTENDANCE REPORT");
            titleCell.setCellStyle(buildTitleStyle(wb));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            // Info rows
            rowNum = addInfoRow(sheet, wb, rowNum, "Subject:", subjectName);
            rowNum = addInfoRow(sheet, wb, rowNum, "Class:",   className);
            rowNum = addInfoRow(sheet, wb, rowNum, "Date:",
                    LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            rowNum++;

            // ---- Summary stats ----
            if (!data.isEmpty()) {
                long pass = data.stream().filter(r -> (Double) r[5] >= 75).count();
                long fail = data.size() - pass;
                double avg = data.stream().mapToDouble(r -> (Double) r[5]).average().orElse(0);

                Row sumRow = sheet.createRow(rowNum++);
                sumRow.setHeightInPoints(22);
                String[] sumLabels = {"Total", "Passed", "Low", "Class Avg"};
                Object[] sumVals   = {data.size(), pass, fail, String.format("%.1f%%", avg)};
                for (int i = 0; i < sumLabels.length; i++) {
                    sumRow.createCell(i * 2).setCellValue(sumLabels[i]);
                    sumRow.createCell(i * 2 + 1).setCellValue(sumVals[i].toString());
                }
                rowNum++;
            }

            // ---- Column headers ----
            Row headerRow = sheet.createRow(rowNum++);
            headerRow.setHeightInPoints(26);
            CellStyle headerStyle = buildHeaderStyle(wb);
            String[] headers = {"Enroll No", "Student Name", "Total", "Attended", "Percentage", "Status"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ---- Data rows ----
            CellStyle normalStyle = buildDataStyle(wb, false);
            CellStyle altStyle    = buildDataStyle(wb, true);
            CellStyle passStyle   = buildStatusStyle(wb, true);
            CellStyle failStyle   = buildStatusStyle(wb, false);

            for (int i = 0; i < data.size(); i++) {
                Object[] rowData = data.get(i);
                Row row  = sheet.createRow(rowNum++);
                row.setHeightInPoints(22);
                boolean alt = i % 2 == 1;
                CellStyle style = alt ? altStyle : normalStyle;

                row.createCell(0).setCellValue((String) rowData[1]);
                row.createCell(1).setCellValue((String) rowData[2]);
                row.createCell(2).setCellValue((int)    rowData[3]);
                row.createCell(3).setCellValue((int)    rowData[4]);
                row.createCell(4).setCellValue(String.format("%.1f%%", (Double) rowData[5]));

                double pct = (Double) rowData[5];
                Cell statusCell = row.createCell(5);
                statusCell.setCellValue(pct >= 75 ? "PASS" : "LOW");
                statusCell.setCellStyle(pct >= 75 ? passStyle : failStyle);

                for (int j = 0; j < 5; j++) {
                    row.getCell(j).setCellStyle(style);
                }
            }

            // Auto-filter
            sheet.setAutoFilter(new CellRangeAddress(rowNum - data.size() - 1,
                    rowNum - 1, 0, 5));

            // Freeze header
            sheet.createFreezePane(0, 6);

            wb.write(out);
        }

        log.info("Excel report generated: {}", filePath);
    }

    // ---- Style builders --------------------------------------------

    private static CellStyle buildTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFColor  bg = new XSSFColor(PRIMARY_RGB, null);
        style.setFillForegroundColor(bg);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
        style.setFont(font);
        return style;
    }

    private static CellStyle buildHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFColor bg = new XSSFColor(PRIMARY_RGB, null);
        style.setFillForegroundColor(bg);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
        style.setFont(font);
        setBorderAll(style, BorderStyle.THIN);
        return style;
    }

    private static CellStyle buildDataStyle(XSSFWorkbook wb, boolean alt) {
        XSSFCellStyle style = wb.createCellStyle();
        if (alt) {
            style.setFillForegroundColor(new XSSFColor(ALT_ROW_RGB, null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorderAll(style, BorderStyle.THIN);
        return style;
    }

    private static CellStyle buildStatusStyle(XSSFWorkbook wb, boolean pass) {
        XSSFCellStyle style = wb.createCellStyle();
        byte[] color = pass ? SUCCESS_RGB : DANGER_RGB;
        style.setFillForegroundColor(new XSSFColor(color, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
        style.setFont(font);
        setBorderAll(style, BorderStyle.THIN);
        return style;
    }

    private static void setBorderAll(CellStyle style, BorderStyle bs) {
        style.setBorderTop(bs);
        style.setBorderBottom(bs);
        style.setBorderLeft(bs);
        style.setBorderRight(bs);
    }

    private static int addInfoRow(XSSFSheet sheet, XSSFWorkbook wb,
                                   int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.setHeightInPoints(18);
        Cell lbl = row.createCell(0);
        lbl.setCellValue(label);
        XSSFCellStyle lblStyle = wb.createCellStyle();
        XSSFFont bold = wb.createFont();
        bold.setBold(true);
        lblStyle.setFont(bold);
        lbl.setCellStyle(lblStyle);
        row.createCell(1).setCellValue(value);
        return rowNum + 1;
    }
}
