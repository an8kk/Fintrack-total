package com.fintrack.backend.service;

import com.fintrack.backend.entity.Transaction;
import com.fintrack.backend.entity.User;
import com.fintrack.backend.repository.TransactionRepository;
import com.fintrack.backend.repository.UserRepository;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exportTransactionsStreaming_Success() throws IOException {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        Transaction t1 = Transaction.builder()
                .id(1L)
                .amount(new BigDecimal("150.00"))
                .date(LocalDateTime.now())
                .description("Test Transaction")
                .currency("USD")
                .category("Food")
                .type(Transaction.TransactionType.EXPENSE)
                .user(user)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(transactionRepository.findByUser(user)).thenReturn(Collections.singletonList(t1));

        StreamingResponseBody stream = reportService.exportTransactionsStreaming();
        assertNotNull(stream);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stream.writeTo(baos);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(baos.toByteArray()))) {
            assertEquals("Transactions", workbook.getSheetName(0));
            assertEquals(1, workbook.getSheetAt(0).getLastRowNum());
            assertEquals("Test Transaction", workbook.getSheetAt(0).getRow(1).getCell(2).getStringCellValue());
        }
    }
}
