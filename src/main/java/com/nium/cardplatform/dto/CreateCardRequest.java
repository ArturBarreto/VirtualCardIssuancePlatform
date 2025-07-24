package com.nium.cardplatform.dto;

import java.math.BigDecimal;

public class CreateCardRequest {
    private String cardholderName;
    private BigDecimal initialBalance;

    // Getters and setters

    public String getCardholderName() {
        return cardholderName;
    }

    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }
}
