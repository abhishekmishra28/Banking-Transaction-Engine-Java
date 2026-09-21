package com.abhishek.banking.application.service;

import java.util.ArrayList;
import java.util.List;

import com.abhishek.banking.domain.enums.FraudRiskLevel;
import com.abhishek.banking.domain.model.FraudAssessment;
import com.abhishek.banking.domain.model.Transaction;
import com.abhishek.banking.domain.value.Money;

public class FraudDetectionService {
    
    // Configurable thresholds for demo purposes
    private final Money largeTransactionThreshold = Money.of("10000.00");
    
    public FraudAssessment assess(Transaction transaction) {
        List<String> triggeredRules = new ArrayList<>();
        FraudRiskLevel riskLevel = FraudRiskLevel.LOW;
        
        if (transaction.getAmount().isGreaterThan(largeTransactionThreshold)) {
            triggeredRules.add("LARGE_TRANSACTION");
            riskLevel = FraudRiskLevel.MEDIUM;
        }
        
        // In a real system, we'd check velocity (e.g. transactions per hour) here using TransactionRepository
        
        String reason = triggeredRules.isEmpty() ? "No suspicious activity detected" : "Triggered rules: " + String.join(", ", triggeredRules);
        
        return new FraudAssessment(transaction.getId(), riskLevel, triggeredRules, reason);
    }
}
