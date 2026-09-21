package com.abhishek.banking.domain.model;

import java.util.List;

import com.abhishek.banking.domain.enums.FraudRiskLevel;

public class FraudAssessment {
    private final String transactionId;
    private final FraudRiskLevel riskLevel;
    private final List<String> triggeredRules;
    private final String reason;

    public FraudAssessment(String transactionId, FraudRiskLevel riskLevel, List<String> triggeredRules, String reason) {
        this.transactionId = transactionId;
        this.riskLevel = riskLevel;
        this.triggeredRules = triggeredRules;
        this.reason = reason;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public FraudRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public List<String> getTriggeredRules() {
        return triggeredRules;
    }

    public String getReason() {
        return reason;
    }
    
    public boolean isRejected() {
        return riskLevel == FraudRiskLevel.HIGH;
    }
}
