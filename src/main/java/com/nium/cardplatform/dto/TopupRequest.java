package com.nium.cardplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Request to top up the card")
public class TopupRequest {
    @Schema(
            description = "Amount to top up. Must be greater than zero.",
            example = "50.00",
            minimum = "0.01"
    )
    private BigDecimal amount;

    public TopupRequest() {} // Default constructor for deserialization

    public TopupRequest(BigDecimal amount) {
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
