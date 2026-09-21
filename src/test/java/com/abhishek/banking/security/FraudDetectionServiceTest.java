package com.abhishek.banking.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.abhishek.banking.application.service.FraudDetectionService;
import com.abhishek.banking.domain.enums.FraudRiskLevel;
import com.abhishek.banking.domain.model.FraudAssessment;
import com.abhishek.banking.domain.model.Transaction;
import com.abhishek.banking.domain.value.Money;

class FraudDetectionServiceTest {

    private final FraudDetectionService fraudDetectionService = new FraudDetectionService();

    @Test
    void testAssess_normalTransaction_lowRisk() {
        Transaction tx = Transaction.createDeposit("ACC-1", Money.of("500.00"));
        FraudAssessment assessment = fraudDetectionService.assess(tx);
        
        assertEquals(FraudRiskLevel.LOW, assessment.getRiskLevel());
        assertTrue(assessment.getTriggeredRules().isEmpty());
        assertFalse(assessment.isRejected());
    }

    @Test
    void testAssess_largeTransaction_mediumRisk() {
        Transaction tx = Transaction.createDeposit("ACC-1", Money.of("15000.00"));
        FraudAssessment assessment = fraudDetectionService.assess(tx);
        
        assertEquals(FraudRiskLevel.MEDIUM, assessment.getRiskLevel());
        assertTrue(assessment.getTriggeredRules().contains("LARGE_TRANSACTION"));
        assertFalse(assessment.isRejected());
    }
}
