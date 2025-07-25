package com.nium.cardplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Details of a transaction on a virtual card")
public class TransactionResponse {
    @Schema(
            description = "Unique transaction identifier",
            example = "ab3cde12-ff45-11ee-be56-0242ac120002"
    )
    private UUID id;

    @Schema(
            description = "Card ID associated with the transaction",
            example = "d290f1ee-6c54-4b01-90e6-d701748f0851"
    )
    private UUID cardId;

    @Schema(
            description = "Transaction type: TOPUP or SPEND",
            example = "SPEND"
    )
    private String type;

    @Schema(
            description = "Transaction amount",
            example = "20.00"
    )
    private BigDecimal amount;

    @Schema(
            description = "Timestamp of transaction (ISO-8601)",
            example = "2025-07-24T16:39:17.425"
    )
    private LocalDateTime createdAt;

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCardId() {
        return cardId;
    }

    public void setCardId(UUID cardId) {
        this.cardId = cardId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
