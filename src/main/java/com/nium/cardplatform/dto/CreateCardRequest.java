package com.nium.cardplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Request to create a new virtual card")
public class CreateCardRequest {
    @Schema(
            description = "Cardholder's full name",
            example = "Alice"
    )
    private String cardholderName;

    @Schema(
            description = "Initial balance to fund the card. Must be zero or positive.",
            example = "100.0",
            minimum = "0"
    )
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
