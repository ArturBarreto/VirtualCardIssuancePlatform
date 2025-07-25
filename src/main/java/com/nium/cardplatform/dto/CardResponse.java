package com.nium.cardplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Details of a virtual card")
public class CardResponse {
    @Schema(
            description = "Unique card identifier",
            example = "d290f1ee-6c54-4b01-90e6-d701748f0851"
    )
    private UUID id;

    @Schema(
            description = "Cardholder's name",
            example = "Alice"
    )
    private String cardholderName;

    @Schema(
            description = "Current balance",
            example = "120.50"
    )
    private BigDecimal balance;

    @Schema(
            description = "Current card status",
            example = "ACTIVE"
    )
    private String status;

    @Schema(
            description = "Card creation timestamp (ISO-8601)",
            example = "2025-07-24T16:34:09.187"
    )
    private LocalDateTime createdAt;

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
