package com.subscriptionmanager.backend.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subscriptionmanager.backend.entity.Payment;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.repository.PaymentRepository;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;

import lombok.RequiredArgsConstructor;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
@RequiredArgsConstructor
public class ExportService {

    private static final DateTimeFormatter GENERATED_AT_FORMAT =
        DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a").withZone(ZoneOffset.UTC);

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public byte[] subscriptionsCsv(Long userId) {
        List<String> headers = List.of(
            "Name", "Category", "Price", "Currency", "Billing Cycle",
            "Status", "Start Date", "Next Billing Date", "Trial", "Trial End Date"
        );
        List<String[]> rows = subscriptionRepository.findByUserIdAndDeletedAtIsNull(userId).stream()
            .map(this::subscriptionRow)
            .toList();
        return buildCsv(headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] subscriptionsPdf(Long userId) {
        List<String> headers = List.of(
            "Name", "Category", "Price", "Cycle", "Status", "Start Date", "Next Billing"
        );
        List<String[]> rows = subscriptionRepository.findByUserIdAndDeletedAtIsNull(userId).stream()
            .map(s -> new String[] {
                s.getName(),
                s.getCategory() == null ? "-" : s.getCategory().getName(),
                s.getPrice() + " " + s.getCurrency(),
                s.getBillingCycle().name(),
                s.getStatus().name(),
                String.valueOf(s.getStartDate()),
                s.getNextBillingDate() == null ? "-" : String.valueOf(s.getNextBillingDate()),
            })
            .toList();
        return buildPdf("Subscriptions", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] paymentsCsv(Long userId) {
        List<String> headers = List.of(
            "Subscription", "Amount", "Currency", "Payment Date", "Status", "Transaction Reference"
        );
        List<String[]> rows = paymentRepository
            .findBySubscriptionUserIdAndDeletedAtIsNullOrderByPaymentDateDesc(userId).stream()
            .map(this::paymentRow)
            .toList();
        return buildCsv(headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] paymentsPdf(Long userId) {
        List<String> headers = List.of("Subscription", "Amount", "Payment Date", "Status", "Reference");
        List<String[]> rows = paymentRepository
            .findBySubscriptionUserIdAndDeletedAtIsNullOrderByPaymentDateDesc(userId).stream()
            .map(p -> new String[] {
                p.getSubscription().getName(),
                p.getAmount() + " " + p.getCurrency(),
                String.valueOf(p.getPaymentDate()),
                p.getStatus().name(),
                p.getTransactionReference() == null ? "-" : p.getTransactionReference(),
            })
            .toList();
        return buildPdf("Payment History", headers, rows);
    }

    private String[] subscriptionRow(Subscription s) {
        return new String[] {
            s.getName(),
            s.getCategory() == null ? "" : s.getCategory().getName(),
            s.getPrice().toPlainString(),
            s.getCurrency(),
            s.getBillingCycle().name(),
            s.getStatus().name(),
            String.valueOf(s.getStartDate()),
            s.getNextBillingDate() == null ? "" : String.valueOf(s.getNextBillingDate()),
            s.isTrial() ? "Yes" : "No",
            s.getTrialEndDate() == null ? "" : String.valueOf(s.getTrialEndDate()),
        };
    }

    private String[] paymentRow(Payment p) {
        return new String[] {
            p.getSubscription().getName(),
            p.getAmount().toPlainString(),
            p.getCurrency(),
            String.valueOf(p.getPaymentDate()),
            p.getStatus().name(),
            p.getTransactionReference() == null ? "" : p.getTransactionReference(),
        };
    }

    private byte[] buildCsv(List<String> headers, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM (U+FEFF) so Excel opens the file as UTF-8 instead of the system codepage
        sb.append((char) 0xFEFF);
        writeCsvRow(sb, headers.toArray(new String[0]));
        for (String[] row : rows) {
            writeCsvRow(sb, row);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void writeCsvRow(StringBuilder sb, String[] fields) {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(csvEscape(fields[i]));
        }
        sb.append("\r\n");
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        boolean needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }

    private byte[] buildPdf(String title, List<String> headers, List<String[]> rows) {
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 32, 24);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.ITALIC);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            Paragraph titleParagraph = new Paragraph(title, titleFont);
            titleParagraph.setSpacingAfter(4);
            document.add(titleParagraph);

            Paragraph meta = new Paragraph(
                "Generated " + GENERATED_AT_FORMAT.format(java.time.Instant.now()) + " UTC - " + rows.size()
                    + (rows.size() == 1 ? " record" : " records"),
                metaFont
            );
            meta.setSpacingAfter(16);
            document.add(meta);

            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Paragraph(header, headerFont));
                cell.setBackgroundColor(new java.awt.Color(0x27, 0x27, 0x27));
                cell.setPadding(6);
                cell.setBorderColor(new java.awt.Color(0xdd, 0xdd, 0xdd));
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(cell);
            }

            if (rows.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Paragraph("No records to show.", cellFont));
                empty.setColspan(headers.size());
                empty.setPadding(10);
                empty.setBorder(Rectangle.NO_BORDER);
                table.addCell(empty);
            } else {
                for (String[] row : rows) {
                    for (String value : row) {
                        PdfPCell cell = new PdfPCell(new Paragraph(value, cellFont));
                        cell.setPadding(5);
                        cell.setBorderColor(new java.awt.Color(0xdd, 0xdd, 0xdd));
                        table.addCell(cell);
                    }
                }
            }

            document.add(table);
            document.close();
        } catch (com.lowagie.text.DocumentException e) {
            throw new UncheckedIOException(new IOException("Failed to generate PDF", e));
        }
        return out.toByteArray();
    }
}
