package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.model.Order;
import org.example.model.OrderItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalInvoiceService {

    private final TemplateEngine templateEngine;

    @Value("${app.storage.local-dir:./local-storage/}")
    private String baseStorageDir;

    public void generateHtmlInvoice(Order order) {
        Context context = new Context();
        context.setVariable("order", order);
        context.setVariable("taxSummary", calculateTaxSummary(order));

        String htmlContent = templateEngine.process("faktura-sablona", context);
        saveInvoiceToFile(order.getOrderNumber(), htmlContent);
    }

    private Map<BigDecimal, Map<String, BigDecimal>> calculateTaxSummary(Order order) {
        Map<BigDecimal, Map<String, BigDecimal>> taxSummary = new HashMap<>();

        for (OrderItem item : order.getItems()) {
            BigDecimal rate = (item.getProduct() != null && item.getProduct().getTaxRate() != null)
                    ? item.getProduct().getTaxRate().getRate() : BigDecimal.ZERO;

            BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
            BigDecimal totalWithTax = item.getUnitPrice().multiply(quantity);

            BigDecimal divisor = BigDecimal.ONE.add(rate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
            BigDecimal base = totalWithTax.divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal tax = totalWithTax.subtract(base);

            taxSummary.putIfAbsent(rate, new HashMap<>(Map.of("base", BigDecimal.ZERO, "tax", BigDecimal.ZERO)));
            Map<String, BigDecimal> amounts = taxSummary.get(rate);
            amounts.put("base", amounts.get("base").add(base));
            amounts.put("tax", amounts.get("tax").add(tax));
        }

        return taxSummary;
    }

    private void saveInvoiceToFile(String orderNumber, String htmlContent) {
        try {
            Path dirPath = Paths.get(baseStorageDir, "invoices").toAbsolutePath().normalize();
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String filename = "FAKTURA-" + orderNumber + ".html";
            Path filePath = dirPath.resolve(filename);

            Files.writeString(filePath, htmlContent, StandardCharsets.UTF_8);
            log.info("Faktura byla vygenerována a uložena jako HTML: {}", filePath.toAbsolutePath());

        } catch (IOException e) {
            log.error("Chyba při generování/ukládání HTML faktury: {}", e.getMessage(), e);
        }
    }

    public ByteArrayInputStream exportInvoiceToExcel(Order order) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Faktura_" + order.getOrderNumber());

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle boldStyle = workbook.createCellStyle();
            boldStyle.setFont(headerFont);

            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("Faktura / Daňový doklad: " + order.getOrderNumber());
            row0.getCell(0).setCellStyle(titleStyle);

            sheet.createRow(2).createCell(0).setCellValue("DODAVATEL:");
            sheet.getRow(2).getCell(0).setCellStyle(headerStyle);
            sheet.createRow(3).createCell(0).setCellValue("FajnDřevo s.r.o.");
            sheet.createRow(4).createCell(0).setCellValue("Truhlářská 123, 602 00 Brno");
            sheet.createRow(5).createCell(0).setCellValue("IČO: 12345678, DIČ: CZ12345678");

            sheet.getRow(2).createCell(3).setCellValue("ODBĚRATEL:");
            sheet.getRow(2).getCell(3).setCellStyle(headerStyle);
            sheet.getRow(3).createCell(3).setCellValue(order.getCustomer() != null ? order.getCustomer().getFullName() : order.getGuestFirstName() + " " + order.getGuestLastName());
            sheet.getRow(4).createCell(3).setCellValue(order.getBillingAddress());

            if (order.getIco() != null && !order.getIco().isBlank()) {
                sheet.getRow(5).createCell(3).setCellValue("IČO: " + order.getIco() + (order.getDic() != null ? ", DIČ: " + order.getDic() : ""));
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            sheet.createRow(7).createCell(0).setCellValue("Datum vystavení:");
            sheet.getRow(7).createCell(1).setCellValue(order.getCreatedAt().format(dtf));

            int rowIdx = 10;
            Row itemHeaderRow = sheet.createRow(rowIdx++);
            String[] columns = {"Položka", "Množství", "Sazba DPH", "Cena za ks", "Celkem s DPH"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = itemHeaderRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            for (OrderItem item : order.getItems()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(item.getProduct().getName());
                row.createCell(1).setCellValue(item.getQuantity() + " " + item.getProduct().getUnit());
                row.createCell(2).setCellValue(item.getProduct().getTaxRate() != null ? item.getProduct().getTaxRate().getRate().doubleValue() + " %" : "0 %");
                row.createCell(3).setCellValue(item.getUnitPrice().doubleValue() + " Kč");
                row.createCell(4).setCellValue(item.getUnitPrice().doubleValue() * item.getQuantity() + " Kč");
            }

            Row shippingRow = sheet.createRow(rowIdx++);
            shippingRow.createCell(0).setCellValue("Doprava a balné");
            shippingRow.createCell(4).setCellValue(order.getShippingCost().doubleValue() + " Kč");

            rowIdx++;
            Row totalRow = sheet.createRow(rowIdx);
            Cell totalLabel = totalRow.createCell(3);
            totalLabel.setCellValue("CELKEM K ÚHRADĚ:");
            totalLabel.setCellStyle(headerStyle);

            Cell totalValue = totalRow.createCell(4);
            totalValue.setCellValue(order.getTotalAmount().doubleValue() + " Kč");
            totalValue.setCellStyle(boldStyle);

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public ByteArrayInputStream exportOrdersToExcel(List<Order> orders) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Přehled Objednávek");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] columns = {
                    "Číslo obj.", "Vytvořeno", "Zákazník", "IČO", "DIČ",
                    "Fakturační adresa", "Stav", "Doprava (Kč)", "Celkem (Kč)"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

            for (Order order : orders) {
                Row row = sheet.createRow(rowIdx++);

                String customerName = order.getCustomer() != null
                        ? order.getCustomer().getFullName()
                        : order.getGuestFirstName() + " " + order.getGuestLastName() + " (Host)";

                row.createCell(0).setCellValue(order.getOrderNumber());
                row.createCell(1).setCellValue(order.getCreatedAt().format(dtf));
                row.createCell(2).setCellValue(customerName);
                row.createCell(3).setCellValue(order.getIco() != null ? order.getIco() : "");
                row.createCell(4).setCellValue(order.getDic() != null ? order.getDic() : "");
                row.createCell(5).setCellValue(order.getBillingAddress() != null ? order.getBillingAddress() : "");
                row.createCell(6).setCellValue(order.getStatus() != null ? order.getStatus().getName() : "Nepřiřazeno");
                row.createCell(7).setCellValue(order.getShippingCost() != null ? order.getShippingCost().doubleValue() : 0.0);
                row.createCell(8).setCellValue(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0);
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}