package com.nium.cardplatform.service;

import com.nium.cardplatform.dto.*;
import com.nium.cardplatform.exception.*;
import com.nium.cardplatform.jooq.tables.records.CardRecord;
import com.nium.cardplatform.jooq.tables.records.TransactionRecord;
import com.nium.cardplatform.repository.CardRepository;
import com.nium.cardplatform.repository.TransactionRepository;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CardService {

    private final CardRepository cardRepo;
    private final TransactionRepository txRepo;
    private final DSLContext dsl;
    private final RateLimiterService rateLimiter;

    public CardService(CardRepository cardRepo, TransactionRepository txRepo, DSLContext dsl, RateLimiterService rateLimiter) {
        this.cardRepo = cardRepo;
        this.txRepo = txRepo;
        this.dsl = dsl;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public CardResponse createCard(CreateCardRequest req) {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(
                cardId,
                req.getCardholderName(),
                req.getInitialBalance(),
                "ACTIVE",
                0, // version
                LocalDateTime.now()
        );
        cardRepo.create(card);

        // Create initial top-up transaction
        if (req.getInitialBalance().compareTo(BigDecimal.ZERO) > 0) {
            TransactionRecord tx = new TransactionRecord(
                    UUID.randomUUID(),
                    cardId,
                    "TOPUP",
                    req.getInitialBalance(),
                    LocalDateTime.now()
            );
            txRepo.create(tx);
        }

        return mapCardToResponse(card);
    }

    @Transactional
    public void spend(UUID cardId, SpendRequest req) {
        CardRecord card = cardRepo.findById(cardId);
        if (card == null) throw new CardNotFoundException("Card not found: " + cardId);
        if (!"ACTIVE".equals(card.getStatus())) throw new CardBlockedException("Card is not active: " + cardId);
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionAmountException("Spend amount must be greater than zero.");
        }

        // Rate limiting check!
        if (!rateLimiter.allowSpend(cardId)) {
            throw new RateLimitExceededException("Max 5 spends per minute exceeded for card: " + cardId);
        }

        BigDecimal newBalance = card.getBalance().subtract(req.getAmount());
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for card: " + cardId);
        }

        int updated = cardRepo.updateBalanceAndVersion(cardId, newBalance, card.getVersion());
        if (updated != 1) {
            throw new ConcurrentModificationException("Concurrent modification detected, try again.");
        }

        TransactionRecord tx = new TransactionRecord(
                UUID.randomUUID(),
                cardId,
                "SPEND",
                req.getAmount(),
                LocalDateTime.now()
        );
        txRepo.create(tx);
    }

    @Transactional
    public void topup(UUID cardId, TopupRequest req) {
        CardRecord card = cardRepo.findById(cardId);
        if (card == null) throw new CardNotFoundException("Card not found: " + cardId);
        if (!"ACTIVE".equals(card.getStatus())) throw new CardBlockedException("Card is not active: " + cardId);
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionAmountException("Top-up amount must be greater than zero.");
        }

        BigDecimal newBalance = card.getBalance().add(req.getAmount());
        int updated = cardRepo.updateBalanceAndVersion(cardId, newBalance, card.getVersion());
        if (updated != 1) {
            throw new ConcurrentModificationException("Concurrent modification detected, try again.");
        }

        TransactionRecord tx = new TransactionRecord(
                UUID.randomUUID(),
                cardId,
                "TOPUP",
                req.getAmount(),
                LocalDateTime.now()
        );
        txRepo.create(tx);
    }

    @Transactional(readOnly = true)
    public CardResponse getCard(UUID cardId) {
        CardRecord card = cardRepo.findById(cardId);
        if (card == null) throw new CardNotFoundException("Card not found: " + cardId);
        return mapCardToResponse(card);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(UUID cardId, int limit, int offset) {
        CardRecord card = cardRepo.findById(cardId);
        if (card == null) throw new CardNotFoundException("Card not found: " + cardId);

        List<TransactionRecord> records = txRepo.findByCardId(cardId, limit, offset);
        if (records == null) records = List.of(); // always return a list

        return records.stream()
                .map(this::mapTxToResponse)
                .collect(Collectors.toList());
    }

    // Helper mapping methods

    private CardResponse mapCardToResponse(CardRecord card) {
        CardResponse resp = new CardResponse();
        resp.setId(card.getId());
        resp.setCardholderName(card.getCardholderName());
        resp.setBalance(card.getBalance());
        resp.setStatus(card.getStatus());
        resp.setCreatedAt(card.getCreatedAt());
        return resp;
    }

    private TransactionResponse mapTxToResponse(TransactionRecord tx) {
        TransactionResponse resp = new TransactionResponse();
        resp.setId(tx.getId());
        resp.setCardId(tx.getCardId());
        resp.setType(tx.getType());
        resp.setAmount(tx.getAmount());
        resp.setCreatedAt(tx.getCreatedAt());
        return resp;
    }

    @Transactional
    public void blockCard(UUID cardId) {
        CardRecord card = cardRepo.findById(cardId);
        if (card == null) throw new CardNotFoundException("Card not found: " + cardId);
        if ("BLOCKED".equals(card.getStatus())) return; // already blocked
        cardRepo.updateStatus(cardId, "BLOCKED");
    }

    @Transactional
    public void unblockCard(UUID cardId) {
        CardRecord card = cardRepo.findById(cardId);
        if (card == null) throw new CardNotFoundException("Card not found: " + cardId);
        if ("ACTIVE".equals(card.getStatus())) return; // already active
        cardRepo.updateStatus(cardId, "ACTIVE");
    }

}
