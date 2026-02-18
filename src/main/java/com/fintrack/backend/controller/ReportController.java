package com.fintrack.backend.controller;

import com.fintrack.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> exportTransactions() {
        log.info("GET /api/reports/export — streaming Excel export");
        StreamingResponseBody stream = reportService.exportTransactionsStreaming();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=FinTrack_Report.xlsx")
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(stream);
    }

    @PostMapping("/import")
    public ResponseEntity<String> importTransactions(@RequestParam("file") MultipartFile file) {
        log.info("POST /api/reports/import — file={}, size={} bytes", file.getOriginalFilename(), file.getSize());
        try {
            reportService.importTransactions(file);
            log.info("Import completed successfully for file={}", file.getOriginalFilename());
            return ResponseEntity.ok("Transactions imported successfully.");
        } catch (IOException e) {
            log.error("Import failed for file={}: {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to import transactions: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Import validation failed for file={}: {}", file.getOriginalFilename(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/sample-csv")
    public ResponseEntity<String> getSampleCsv() {
        log.info("GET /api/reports/sample-csv");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fintrack_sample.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(reportService.generateSampleCsv());
    }
}
