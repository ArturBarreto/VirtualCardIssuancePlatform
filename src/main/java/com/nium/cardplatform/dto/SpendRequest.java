package com.nium.cardplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Request to spend from the card")
public class SpendRequest {
    @Schema(
            description = "Amount to spend. Must be greater than zero.",
            example = "30.00",
            minimum = "0.01"
    )
    private BigDecimal amount;

    public SpendRequest() {} // Default constructor for deserialization

    public SpendRequest(BigDecimal amount) {
        this.amount = amount;
    }

    // Getters and setters

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
