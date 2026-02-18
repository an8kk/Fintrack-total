package com.fintrack.backend.service;

import com.fintrack.backend.entity.Transaction;
import com.fintrack.backend.entity.User;
import com.fintrack.backend.repository.TransactionRepository;
import com.fintrack.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("No authenticated user found");
        }
        // SecurityContext principal is the EMAIL (set by CustomUserDetailsService)
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found for email: " + auth.getName()));
    }

    public StreamingResponseBody exportTransactionsStreaming() {
        User user = getAuthenticatedUser();
        List<Transaction> transactions = transactionRepository.findByUser(user);

        return outputStream -> {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Transactions");

                // Header
                Row headerRow = sheet.createRow(0);
                String[] columns = { "ID", "Date", "Description", "Amount", "Currency", "Category", "Type" };
                for (int i = 0; i < columns.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns[i]);
                }

                // Data rows
                int rowIdx = 1;
                for (Transaction transaction : transactions) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(transaction.getId() != null ? transaction.getId() : 0);
                    row.createCell(1)
                            .setCellValue(transaction.getDate() != null ? transaction.getDate().toString() : "");
                    row.createCell(2)
                            .setCellValue(transaction.getDescription() != null ? transaction.getDescription() : "");
                    row.createCell(3).setCellValue(
                            transaction.getAmount() != null ? transaction.getAmount().doubleValue() : 0.0);
                    row.createCell(4)
                            .setCellValue(transaction.getCurrency() != null ? transaction.getCurrency() : "KZT");
                    row.createCell(5).setCellValue(transaction.getCategory() != null ? transaction.getCategory() : "");
                    row.createCell(6).setCellValue(transaction.getType() != null ? transaction.getType().name() : "");
                }

                workbook.write(outputStream);
                outputStream.flush();
            }
        };
    }

    public void importTransactions(MultipartFile file) throws IOException {
        User user = getAuthenticatedUser();

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File has no name");
        }

        if (filename.endsWith(".csv")) {
            importCsv(file, user);
        } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            importExcel(file, user);
        } else {
            throw new IllegalArgumentException("Unsupported file format. Use .csv, .xls, or .xlsx");
        }
    }

    private void importCsv(MultipartFile file, User user) throws IOException {
        try (BufferedReader fileReader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                CSVParser csvParser = new CSVParser(fileReader,
                        CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            List<Transaction> transactions = new ArrayList<>();

            for (CSVRecord csvRecord : csvParser.getRecords()) {
                Transaction transaction = parseTransaction(csvRecord.toMap(), user);
                if (transaction != null) {
                    transactions.add(transaction);
                }
            }

            if (!transactions.isEmpty()) {
                transactionRepository.saveAll(transactions);
                log.info("Imported {} transactions from CSV for user {}", transactions.size(), user.getEmail());
            }
        }
    }

    private void importExcel(MultipartFile file, User user) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            List<Transaction> transactions = new ArrayList<>();

            int rowNumber = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();
                if (rowNumber == 0) { // Skip header
                    rowNumber++;
                    continue;
                }

                Transaction transaction = parseExcelRow(currentRow, user);
                if (transaction != null) {
                    transactions.add(transaction);
                }
            }

            if (!transactions.isEmpty()) {
                transactionRepository.saveAll(transactions);
                log.info("Imported {} transactions from Excel for user {}", transactions.size(), user.getEmail());
            }
        }
    }

    private Transaction parseTransaction(java.util.Map<String, String> record, User user) {
        try {
            String amountStr = record.get("Amount");
            String dateStr = record.get("Date");
            String desc = record.get("Description");

            if (amountStr == null || amountStr.isBlank() || dateStr == null || dateStr.isBlank()) {
                return null;
            }

            BigDecimal amount = new BigDecimal(amountStr.trim());
            LocalDateTime date = parseDate(dateStr.trim());

            // Determine type: explicit column or infer from amount sign
            Transaction.TransactionType type;
            String typeStr = record.get("Type");
            if (typeStr != null && !typeStr.isBlank()) {
                type = Transaction.TransactionType.valueOf(typeStr.trim().toUpperCase());
            } else {
                type = amount.signum() > 0 ? Transaction.TransactionType.INCOME : Transaction.TransactionType.EXPENSE;
            }

            return Transaction.builder()
                    .user(user)
                    .amount(amount.abs())
                    .date(date)
                    .description(desc != null ? desc.trim() : "")
                    .currency(record.getOrDefault("Currency", "KZT"))
                    .category(record.getOrDefault("Category", "Uncategorized"))
                    .type(type)
                    .build();
        } catch (Exception e) {
            log.error("Error parsing CSV record: {}", e.getMessage());
            return null;
        }
    }

    private Transaction parseExcelRow(Row row, User user) {
        try {
            Cell dateCell = row.getCell(1);
            Cell descCell = row.getCell(2);
            Cell amountCell = row.getCell(3);
            Cell currCell = row.getCell(4);
            Cell catCell = row.getCell(5);
            Cell typeCell = row.getCell(6);

            if (dateCell == null || amountCell == null)
                return null;

            LocalDateTime date;
            try {
                date = LocalDateTime.parse(getCellStringValue(dateCell));
            } catch (Exception e) {
                date = LocalDateTime.now();
            }

            BigDecimal amount;
            if (amountCell.getCellType() == CellType.NUMERIC) {
                amount = new BigDecimal(amountCell.getNumericCellValue());
            } else {
                amount = new BigDecimal(amountCell.getStringCellValue().trim());
            }

            Transaction.TransactionType type;
            String typeStr = getCellStringValue(typeCell);
            if (typeStr != null && !typeStr.isBlank()) {
                try {
                    type = Transaction.TransactionType.valueOf(typeStr.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    type = amount.signum() > 0 ? Transaction.TransactionType.INCOME
                            : Transaction.TransactionType.EXPENSE;
                }
            } else {
                type = amount.signum() > 0 ? Transaction.TransactionType.INCOME : Transaction.TransactionType.EXPENSE;
            }

            return Transaction.builder()
                    .user(user)
                    .amount(amount.abs())
                    .date(date)
                    .description(getCellStringValue(descCell))
                    .currency(currCell != null ? getCellStringValue(currCell) : "KZT")
                    .category(catCell != null ? getCellStringValue(catCell) : "Uncategorized")
                    .type(type)
                    .build();

        } catch (Exception e) {
            log.error("Error parsing Excel row: {}", e.getMessage());
            return null;
        }
    }

    public String generateSampleCsv() {
        return "Date,Description,Amount,Currency,Category,Type\n"
                + "2025-01-15T10:30:00,Monthly Salary,500000,KZT,Salary,INCOME\n"
                + "2025-01-16T12:00:00,Grocery Shopping,-15000,KZT,Food,EXPENSE\n"
                + "2025-01-17T08:45:00,Uber Ride,-2500,KZT,Transport,EXPENSE\n"
                + "2025-01-18T19:30:00,Netflix Subscription,-3500,KZT,Entertainment,EXPENSE\n"
                + "2025-01-20T14:00:00,Freelance Payment,75000,KZT,Salary,INCOME\n";
    }

    private LocalDateTime parseDate(String dateStr) {
        // Try multiple formats
        try {
            return LocalDateTime.parse(dateStr);
        } catch (DateTimeParseException e) {
            // Try date-only format
            try {
                return LocalDateTime.parse(dateStr + "T00:00:00");
            } catch (DateTimeParseException e2) {
                try {
                    return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                } catch (DateTimeParseException e3) {
                    return LocalDateTime.now();
                }
            }
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null)
            return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
