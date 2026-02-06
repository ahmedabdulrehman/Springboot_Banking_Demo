package com.dailybanking.transaction.integration;

import com.dailybanking.transaction.dto.DepositRequest;
import com.dailybanking.transaction.dto.TransactionEvent;
import com.dailybanking.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class TransactionServiceIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @MockBean
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Test
    void shouldProcessDepositEndToEnd() {
        DepositRequest request = new DepositRequest();
        request.setAccountId("acc-int-1");
        request.setAmount(BigDecimal.valueOf(75));
        request.setCurrency("EUR");

        var response = transactionService.deposit(request);

        assertEquals("DEPOSIT", response.getType().name());
        assertEquals("COMPLETED", response.getStatus().name());
    }
}
