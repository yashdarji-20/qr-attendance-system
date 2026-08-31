package com.attendance.service;

import com.attendance.dao.LectureDAO;
import com.attendance.dao.QRTokenDAO;
import com.attendance.model.Lecture;
import com.attendance.model.QRToken;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/**
 * Service responsible for:
 *  - Generating QR codes with embedded attendance tokens
 *  - Decoding QR codes scanned by students
 *  - Managing token expiry (30-second window)
 */
public class QRCodeService {

    private static final Logger log = LoggerFactory.getLogger(QRCodeService.class);

    // QR code dimensions
    private static final int QR_WIDTH  = 400;
    private static final int QR_HEIGHT = 400;

    // QR expiry (configurable, default 30 seconds)
    private static final int DEFAULT_EXPIRY_SECONDS = 30;

    // JSON key constants in QR payload
    private static final String KEY_TOKEN       = "token";
    private static final String KEY_LECTURE_ID  = "lectureId";
    private static final String KEY_SUBJECT_ID  = "subjectId";
    private static final String KEY_TEACHER_ID  = "teacherId";
    private static final String KEY_CLASS_ID    = "classId";
    private static final String KEY_TIMESTAMP   = "ts";
    private static final String KEY_EXPIRES_AT  = "exp";

    private final QRTokenDAO  qrTokenDAO;
    private final LectureDAO  lectureDAO;

    public QRCodeService() {
        this.qrTokenDAO = new QRTokenDAO();
        this.lectureDAO = new LectureDAO();
    }

    // ---- QR Generation -----------------------------------------------

    /**
     * Generate a new QR code for a lecture.
     * Old tokens for the same lecture are expired first.
     * Returns a QRToken with the generated image attached.
     *
     * @param lectureId  the lecture to generate QR for
     * @param teacherId  the teacher requesting generation
     * @param expirySeconds how many seconds before QR expires (0 = use default 30s)
     */
    public GenerationResult generateQR(int lectureId, int teacherId, int expirySeconds)
            throws Exception {

        // Expire previous tokens for this lecture
        qrTokenDAO.expireTokensForLecture(lectureId);

        // Fetch lecture details
        Lecture lecture = lectureDAO.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("Lecture not found: " + lectureId));

        if (expirySeconds <= 0) expirySeconds = DEFAULT_EXPIRY_SECONDS;

        // Build token
        String tokenUUID = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        Instant now      = Instant.now();
        Instant expiry   = now.plusSeconds(expirySeconds);

        // Build JSON payload (minimal, no library needed)
        String payload = buildPayload(tokenUUID, lecture, expiry);

        // Persist token
        QRToken qrToken = new QRToken();
        qrToken.setLectureId (lectureId);
        qrToken.setTokenValue(tokenUUID);
        qrToken.setQrData    (payload);
        qrToken.setExpiresAt (Timestamp.from(expiry));
        qrTokenDAO.insert(qrToken);

        // Mark lecture as ongoing
        lectureDAO.updateStatus(lectureId, Lecture.LectureStatus.ONGOING);

        // Generate QR image
        BufferedImage qrImage = createQRImage(payload);

        log.info("QR generated for lecture={}, token={}, expires={}s",
                lectureId, tokenUUID, expirySeconds);

        return new GenerationResult(qrToken, qrImage, expirySeconds);
    }

    /**
     * Build a simple JSON-like payload string embedded in the QR.
     */
    private String buildPayload(String token, Lecture lecture, Instant expiry) {
        return "{" +
            "\"" + KEY_TOKEN      + "\":\"" + token                    + "\"," +
            "\"" + KEY_LECTURE_ID + "\":"   + lecture.getLectureId()   + ","   +
            "\"" + KEY_SUBJECT_ID + "\":"   + lecture.getSubjectId()   + ","   +
            "\"" + KEY_TEACHER_ID + "\":"   + lecture.getTeacherId()   + ","   +
            "\"" + KEY_CLASS_ID   + "\":"   + lecture.getClassId()     + ","   +
            "\"" + KEY_TIMESTAMP  + "\":"   + System.currentTimeMillis()       +
            "}";
    }

    /**
     * Create a BufferedImage QR code from the given text.
     */
    private BufferedImage createQRImage(String content) throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE,
                QR_WIDTH, QR_HEIGHT, hints);

        // Dark blue on white
        MatrixToImageConfig config = new MatrixToImageConfig(0xFF1A3C6E, 0xFFFFFFFF);
        return MatrixToImageWriter.toBufferedImage(matrix, config);
    }

    // ---- QR Decoding -------------------------------------------------

    /**
     * Decode a QR code from a BufferedImage.
     *
     * @return the raw text payload, or null if decoding fails
     */
    public String decodeQRImage(BufferedImage image) {
        if (image == null) return null;
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap    bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.POSSIBLE_FORMATS,
                    Collections.singletonList(BarcodeFormat.QR_CODE));
            Result result = new MultiFormatReader().decode(bitmap, hints);
            return result.getText();
        } catch (NotFoundException e) {
            log.warn("No QR code found in image.");
            return null;
        } catch (Exception e) {
            log.error("QR decode error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse the JSON payload from a decoded QR string.
     *
     * @return map of key->value pairs
     */
    public Map<String, String> parsePayload(String payload) {
        Map<String, String> map = new LinkedHashMap<>();
        if (payload == null || payload.isBlank()) return map;

        // Simple JSON parser (avoids external lib)
        String cleaned = payload.trim();
        if (cleaned.startsWith("{")) cleaned = cleaned.substring(1);
        if (cleaned.endsWith("}"))  cleaned = cleaned.substring(0, cleaned.length() - 1);

        for (String pair : cleaned.split(",")) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                String val = kv[1].trim().replace("\"", "");
                map.put(key, val);
            }
        }
        return map;
    }

    /**
     * Extract the token UUID from a decoded QR payload string.
     */
    public String extractToken(String payload) {
        return parsePayload(payload).getOrDefault(KEY_TOKEN, null);
    }

    /**
     * Validate a QR token from the database.
     * Checks: exists, not expired by time, not flagged expired.
     */
    public ValidationResult validateToken(String tokenValue) throws Exception {
        // First expire stale tokens
        qrTokenDAO.expireOldTokens();

        Optional<QRToken> opt = qrTokenDAO.findByTokenValue(tokenValue);
        if (opt.isEmpty()) {
            return new ValidationResult(false, null, "Invalid QR code. Token not found.");
        }

        QRToken token = opt.get();

        if (token.isExpired()) {
            return new ValidationResult(false, token, "QR code has expired. Please ask your teacher to regenerate.");
        }

        if (!token.isValid()) {
            return new ValidationResult(false, token, "QR code is no longer valid.");
        }

        return new ValidationResult(true, token, "Valid QR code.");
    }

    // ---- Nested Result Classes ----------------------------------------

    /**
     * Encapsulates the result of QR code generation.
     */
    public static final class GenerationResult {
        private final QRToken       qrToken;
        private final BufferedImage qrImage;
        private final int           expirySeconds;

        public GenerationResult(QRToken t, BufferedImage img, int exp) {
            this.qrToken      = t;
            this.qrImage      = img;
            this.expirySeconds = exp;
        }

        public QRToken       getQrToken()      { return qrToken; }
        public BufferedImage getQrImage()      { return qrImage; }
        public int           getExpirySeconds(){ return expirySeconds; }
    }

    /**
     * Encapsulates the result of QR token validation.
     */
    public static final class ValidationResult {
        private final boolean valid;
        private final QRToken token;
        private final String  message;

        public ValidationResult(boolean valid, QRToken token, String message) {
            this.valid   = valid;
            this.token   = token;
            this.message = message;
        }

        public boolean isValid()    { return valid; }
        public QRToken getToken()   { return token; }
        public String  getMessage() { return message; }
    }
}
