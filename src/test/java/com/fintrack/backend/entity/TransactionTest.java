package com.fintrack.backend.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    // Verify Builder and Enum functionality works (Data Integrity)
    @Test
    void testTransactionBuilderAndType() {
        Transaction transaction = Transaction.builder()
                .amount(new BigDecimal("25.50"))
                .category("Food")
                .type(Transaction.TransactionType.EXPENSE)
                .build();

        assertNotNull(transaction);
        assertEquals(Transaction.TransactionType.EXPENSE, transaction.getType());
        assertEquals(new BigDecimal("25.50"), transaction.getAmount());
        assertEquals("Food", transaction.getCategory());
    }
}