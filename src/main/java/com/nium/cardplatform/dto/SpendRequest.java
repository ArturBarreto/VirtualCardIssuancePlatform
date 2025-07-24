package com.nium.cardplatform.dto;

import java.math.BigDecimal;

public class SpendRequest {
    private BigDecimal amount;

    // Getters and setters

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
