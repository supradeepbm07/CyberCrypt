package com.cybercrypt.service;

import com.cybercrypt.model.CaseFile;
import com.cybercrypt.model.CustodyEvent;
import com.cybercrypt.model.Evidence;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.MultiFormatWriter;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class ReportService {
    public byte[] qr(String text) throws Exception {
        BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 260, 260);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    public byte[] pdf(CaseFile c, List<Evidence> evidence, List<CustodyEvent> events) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = 770;
                cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
                cs.beginText(); cs.newLineAtOffset(50, y); cs.showText("CYBERCRYPT - CASE REPORT"); cs.endText();
                y -= 35;
                cs.setFont(PDType1Font.HELVETICA, 10);

                String[] lines = {
                    "Tracking ID: " + c.getTrackingId(),
                    "Status: " + c.getStatus(),
                    "Priority: " + c.getPriority() + " | Risk Score: " + c.getRiskScore() + "/100",
                    "Crime Type: " + safe(c.getCrimeType()),
                    "Anonymous: " + c.isAnonymous(),
                    "Reporter: " + (c.isAnonymous() ? "ANONYMOUS" : safe(c.getReporterName())),
                    "Financial Loss: " + (c.getFinancialLoss() == null ? "0" : c.getFinancialLoss()),
                    "Created: " + c.getCreatedAt(),
                    "Assigned Officer: " + safe(c.getAssignedOfficer()),
                    "Verdict: " + safe(c.getVerdict())
                };
                for (String line : lines) {
                    cs.beginText(); cs.newLineAtOffset(50, y); cs.showText(line); cs.endText(); y -= 17;
                }

                y -= 10;
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.beginText(); cs.newLineAtOffset(50, y); cs.showText("Description"); cs.endText(); y -= 18;
                cs.setFont(PDType1Font.HELVETICA, 9);
                for (String part : wrap(safe(c.getDescription()), 105)) {
                    cs.beginText(); cs.newLineAtOffset(50, y); cs.showText(part); cs.endText(); y -= 13;
                }

                y -= 8;
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.beginText(); cs.newLineAtOffset(50, y); cs.showText("Evidence"); cs.endText(); y -= 17;
                cs.setFont(PDType1Font.HELVETICA, 8);
                for (Evidence e : evidence) {
                    String line = e.getOriginalFileName() + " | SHA-256: " + e.getSha256();
                    for (String part : wrap(line, 115)) {
                        cs.beginText(); cs.newLineAtOffset(50, y); cs.showText(part); cs.endText(); y -= 12;
                    }
                    y -= 3;
                    if (y < 130) break;
                }

                if (y > 120) {
                    y -= 8;
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    cs.beginText(); cs.newLineAtOffset(50, y); cs.showText("Chain of Custody"); cs.endText(); y -= 17;
                    cs.setFont(PDType1Font.HELVETICA, 8);
                    for (CustodyEvent e : events) {
                        String line = e.getEventTime() + " | " + e.getActor() + " | " + e.getAction();
                        for (String part : wrap(line, 115)) {
                            cs.beginText(); cs.newLineAtOffset(50, y); cs.showText(part); cs.endText(); y -= 12;
                        }
                        if (y < 55) break;
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private String safe(String s) { return s == null ? "-" : s.replaceAll("[\\r\\n]", " "); }

    private List<String> wrap(String text, int max) {
        if (text == null || text.isBlank()) return List.of("-");
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (cur.length() + word.length() + 1 > max) {
                lines.add(cur.toString());
                cur.setLength(0);
            }
            if (cur.length() > 0) cur.append(' ');
            cur.append(word);
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }
}
