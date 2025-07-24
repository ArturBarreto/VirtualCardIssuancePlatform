package com.nium.cardplatform.dto;

import java.math.BigDecimal;

public class TopupRequest {
    private BigDecimal amount;

    // Getters and setters

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
