package com.nium.cardplatform.service;

import com.nium.cardplatform.dto.*;
import com.nium.cardplatform.exception.*;
import com.nium.cardplatform.jooq.tables.records.CardRecord;
import com.nium.cardplatform.jooq.tables.records.TransactionRecord;
import com.nium.cardplatform.repository.CardRepository;
import com.nium.cardplatform.repository.TransactionRepository;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CardServiceTest {

    private CardRepository cardRepo;
    private TransactionRepository txRepo;
    private DSLContext dsl;
    private RateLimiterService rateLimiter;
    private CardService cardService;

    @BeforeEach
    void setup() {
        cardRepo = mock(CardRepository.class);
        txRepo = mock(TransactionRepository.class);
        dsl = mock(DSLContext.class);
        rateLimiter = mock(RateLimiterService.class);
        cardService = new CardService(cardRepo, txRepo, dsl, rateLimiter);
    }

    @Test
    void createCard_shouldCreateCardAndInitialTransaction_whenInitialBalancePositive() {
        CreateCardRequest req = new CreateCardRequest();
        req.setCardholderName("John Doe");
        req.setInitialBalance(BigDecimal.valueOf(100));

        ArgumentCaptor<CardRecord> cardCaptor = ArgumentCaptor.forClass(CardRecord.class);
        ArgumentCaptor<TransactionRecord> txCaptor = ArgumentCaptor.forClass(TransactionRecord.class);

        CardResponse response = cardService.createCard(req);

        verify(cardRepo).create(cardCaptor.capture());
        CardRecord savedCard = cardCaptor.getValue();
        assertEquals("John Doe", savedCard.getCardholderName());
        assertEquals(BigDecimal.valueOf(100), savedCard.getBalance());
        assertEquals("ACTIVE", savedCard.getStatus());

        verify(txRepo).create(txCaptor.capture());
        TransactionRecord savedTx = txCaptor.getValue();
        assertEquals("TOPUP", savedTx.getType());
        assertEquals(BigDecimal.valueOf(100), savedTx.getAmount());
        assertEquals(savedCard.getId(), savedTx.getCardId());
    }

    @Test
    void createCard_shouldThrowException_whenInitialBalanceNegativeOrNull() {
        CreateCardRequest req = new CreateCardRequest();
        req.setCardholderName("Jane Doe");
        req.setInitialBalance(BigDecimal.valueOf(-10));

        assertThrows(InvalidTransactionAmountException.class, () -> cardService.createCard(req));

        req.setInitialBalance(null);
        assertThrows(InvalidTransactionAmountException.class, () -> cardService.createCard(req));
    }

    @Test
    void spend_shouldSucceed_whenValidRequest() {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(cardId, "Holder", BigDecimal.valueOf(100), "ACTIVE", 0, LocalDateTime.now());

        when(cardRepo.findById(cardId)).thenReturn(card);
        when(rateLimiter.allowSpend(cardId)).thenReturn(true);
        when(cardRepo.updateBalanceAndVersion(eq(cardId), any(), eq(0))).thenReturn(1);

        SpendRequest req = new SpendRequest();
        req.setAmount(BigDecimal.valueOf(30));
        cardService.spend(cardId, req);

        verify(txRepo).create(any(TransactionRecord.class));
        verify(cardRepo).updateBalanceAndVersion(cardId, BigDecimal.valueOf(70), 0);
    }

    @Test
    void spend_shouldThrow_whenCardNotFound() {
        UUID cardId = UUID.randomUUID();
        when(cardRepo.findById(cardId)).thenReturn(null);

        SpendRequest req = new SpendRequest();
        req.setAmount(BigDecimal.valueOf(10));

        assertThrows(CardNotFoundException.class, () -> cardService.spend(cardId, req));
    }

    @Test
    void spend_shouldThrow_whenCardInactive() {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(cardId, "Holder", BigDecimal.valueOf(100), "BLOCKED", 0, LocalDateTime.now());
        when(cardRepo.findById(cardId)).thenReturn(card);

        SpendRequest req = new SpendRequest();
        req.setAmount(BigDecimal.valueOf(10));

        assertThrows(CardBlockedException.class, () -> cardService.spend(cardId, req));
    }

    @Test
    void spend_shouldThrow_whenInsufficientBalance() {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(cardId, "Holder", BigDecimal.valueOf(10), "ACTIVE", 0, LocalDateTime.now());
        when(cardRepo.findById(cardId)).thenReturn(card);
        when(rateLimiter.allowSpend(cardId)).thenReturn(true);

        SpendRequest req = new SpendRequest();
        req.setAmount(BigDecimal.valueOf(20));

        assertThrows(InsufficientBalanceException.class, () -> cardService.spend(cardId, req));
    }

    @Test
    void topup_shouldIncreaseBalanceAndCreateTransaction_whenValidRequest() {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(cardId, "User", BigDecimal.valueOf(50), "ACTIVE", 1, LocalDateTime.now());
        TopupRequest req = new TopupRequest();
        req.setAmount(BigDecimal.valueOf(25));
        when(cardRepo.findById(cardId)).thenReturn(card);
        when(cardRepo.updateBalanceAndVersion(cardId, BigDecimal.valueOf(75), 1)).thenReturn(1);

        cardService.topup(cardId, req);

        verify(cardRepo).updateBalanceAndVersion(cardId, BigDecimal.valueOf(75), 1);
        verify(txRepo).create(any(TransactionRecord.class));
    }

    @Test
    void topup_shouldThrowException_whenAmountInvalid() {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(cardId, "User", BigDecimal.valueOf(50), "ACTIVE", 1, LocalDateTime.now());
        TopupRequest req = new TopupRequest();
        req.setAmount(BigDecimal.valueOf(0));
        when(cardRepo.findById(cardId)).thenReturn(card);

        assertThrows(InvalidTransactionAmountException.class, () -> cardService.topup(cardId, req));
    }

    @Test
    void topup_shouldThrowException_whenCardNotFound() {
        UUID cardId = UUID.randomUUID();
        TopupRequest req = new TopupRequest();
        req.setAmount(BigDecimal.valueOf(10));
        when(cardRepo.findById(cardId)).thenReturn(null);

        assertThrows(CardNotFoundException.class, () -> cardService.topup(cardId, req));
    }

    @Test
    void topup_shouldThrowException_whenCardInactive() {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(cardId, "User", BigDecimal.valueOf(50), "BLOCKED", 1, LocalDateTime.now());
        TopupRequest req = new TopupRequest();
        req.setAmount(BigDecimal.valueOf(10));
        when(cardRepo.findById(cardId)).thenReturn(card);

        assertThrows(CardBlockedException.class, () -> cardService.topup(cardId, req));
    }

    @Test
    void topup_shouldThrowException_whenConcurrentModificationDetected() {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(cardId, "User", BigDecimal.valueOf(50), "ACTIVE", 1, LocalDateTime.now());
        TopupRequest req = new TopupRequest();
        req.setAmount(BigDecimal.valueOf(10));
        when(cardRepo.findById(cardId)).thenReturn(card);
        when(cardRepo.updateBalanceAndVersion(cardId, BigDecimal.valueOf(60), 1)).thenReturn(0);

        assertThrows(ConcurrentModificationException.class, () -> cardService.topup(cardId, req));
    }

    @Test
    void getCard_shouldReturnCard_whenExists() {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(cardId, "User", BigDecimal.valueOf(100), "ACTIVE", 1, LocalDateTime.now());
        when(cardRepo.findById(cardId)).thenReturn(card);

        CardResponse resp = cardService.getCard(cardId);

        assertEquals(cardId, resp.getId());
        assertEquals("User", resp.getCardholderName());
    }

    @Test
    void getCard_shouldThrowException_whenCardNotFound() {
        UUID cardId = UUID.randomUUID();
        when(cardRepo.findById(cardId)).thenReturn(null);

        assertThrows(CardNotFoundException.class, () -> cardService.getCard(cardId));
    }

    @Test
    void getTransactions_shouldReturnTransactions_whenExists() {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(cardId, "User", BigDecimal.valueOf(100), "ACTIVE", 1, LocalDateTime.now());
        TransactionRecord tx = new TransactionRecord(UUID.randomUUID(), cardId, "SPEND", BigDecimal.valueOf(10), LocalDateTime.now());
        when(cardRepo.findById(cardId)).thenReturn(card);
        when(txRepo.findByCardId(cardId, 10, 0)).thenReturn(List.of(tx));

        List<TransactionResponse> resp = cardService.getTransactions(cardId, 10, 0);

        assertEquals(1, resp.size());
        assertEquals("SPEND", resp.get(0).getType());
    }

    @Test
    void getTransactions_shouldReturnEmptyList_whenNullReturned() {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(cardId, "User", BigDecimal.valueOf(100), "ACTIVE", 1, LocalDateTime.now());
        when(cardRepo.findById(cardId)).thenReturn(card);
        when(txRepo.findByCardId(cardId, 10, 0)).thenReturn(null);

        List<TransactionResponse> resp = cardService.getTransactions(cardId, 10, 0);

        assertNotNull(resp);
        assertTrue(resp.isEmpty());
    }

    @Test
    void getTransactions_shouldThrowException_whenCardNotFound() {
        UUID cardId = UUID.randomUUID();
        when(cardRepo.findById(cardId)).thenReturn(null);

        assertThrows(CardNotFoundException.class, () -> cardService.getTransactions(cardId, 10, 0));
    }

    @Test
    void blockCard_shouldUpdateStatus_whenActive() {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(cardId, "User", BigDecimal.valueOf(100), "ACTIVE", 1, LocalDateTime.now());
        when(cardRepo.findById(cardId)).thenReturn(card);

        cardService.blockCard(cardId);

        verify(cardRepo).updateStatus(cardId, "BLOCKED");
    }

    @Test
    void blockCard_shouldDoNothing_whenAlreadyBlocked() {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(cardId, "User", BigDecimal.valueOf(100), "BLOCKED", 1, LocalDateTime.now());
        when(cardRepo.findById(cardId)).thenReturn(card);

        cardService.blockCard(cardId);

        verify(cardRepo, never()).updateStatus(any(), any());
    }

    @Test
    void blockCard_shouldThrowException_whenCardNotFound() {
        UUID cardId = UUID.randomUUID();
        when(cardRepo.findById(cardId)).thenReturn(null);

        assertThrows(CardNotFoundException.class, () -> cardService.blockCard(cardId));
    }

    @Test
    void unblockCard_shouldUpdateStatus_whenBlocked() {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(cardId, "User", BigDecimal.valueOf(100), "BLOCKED", 1, LocalDateTime.now());
        when(cardRepo.findById(cardId)).thenReturn(card);

        cardService.unblockCard(cardId);

        verify(cardRepo).updateStatus(cardId, "ACTIVE");
    }

    @Test
    void unblockCard_shouldDoNothing_whenAlreadyActive() {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(cardId, "User", BigDecimal.valueOf(100), "ACTIVE", 1, LocalDateTime.now());
        when(cardRepo.findById(cardId)).thenReturn(card);

        cardService.unblockCard(cardId);

        verify(cardRepo, never()).updateStatus(any(), any());
    }

    @Test
    void unblockCard_shouldThrowException_whenCardNotFound() {
        UUID cardId = UUID.randomUUID();
        when(cardRepo.findById(cardId)).thenReturn(null);

        assertThrows(CardNotFoundException.class, () -> cardService.unblockCard(cardId));
    }

    // --- Create Card: Edge cases for cardholder name ---
    @Test
    void createCard_shouldCreateWithEmptyName_ifAllowed() {
        CreateCardRequest req = new CreateCardRequest();
        req.setCardholderName(""); // Empty
        req.setInitialBalance(BigDecimal.ZERO);

        // No exception expected if business logic allows
        cardService.createCard(req);
        verify(cardRepo).create(any(CardRecord.class));
    }

    @Test
    void createCard_shouldCreateWithSpecialCharsInName_ifAllowed() {
        CreateCardRequest req = new CreateCardRequest();
        req.setCardholderName("@rtur $ilva-测试");
        req.setInitialBalance(BigDecimal.TEN);

        cardService.createCard(req);
        verify(cardRepo).create(any(CardRecord.class));
    }

    // --- Spend: amount is null, zero, or negative ---
    @Test
    void spend_shouldThrow_whenAmountIsNull() {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(cardId, "User", BigDecimal.TEN, "ACTIVE", 1, LocalDateTime.now());
        SpendRequest req = new SpendRequest();
        req.setAmount(null);
        when(cardRepo.findById(cardId)).thenReturn(card);

        assertThrows(InvalidTransactionAmountException.class, () -> cardService.spend(cardId, req));
    }

    @Test
    void spend_shouldThrow_whenAmountIsZeroOrNegative() {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(cardId, "User", BigDecimal.TEN, "ACTIVE", 1, LocalDateTime.now());
        SpendRequest reqZero = new SpendRequest();
        reqZero.setAmount(BigDecimal.ZERO);
        SpendRequest reqNegative = new SpendRequest();
        reqNegative.setAmount(BigDecimal.valueOf(-5));
        when(cardRepo.findById(cardId)).thenReturn(card);

        assertThrows(InvalidTransactionAmountException.class, () -> cardService.spend(cardId, reqZero));
        assertThrows(InvalidTransactionAmountException.class, () -> cardService.spend(cardId, reqNegative));
    }

    // --- Topup: null amount ---
    @Test
    void topup_shouldThrow_whenAmountIsNull() {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(cardId, "User", BigDecimal.TEN, "ACTIVE", 1, LocalDateTime.now());
        TopupRequest req = new TopupRequest();
        req.setAmount(null);
        when(cardRepo.findById(cardId)).thenReturn(card);

        assertThrows(InvalidTransactionAmountException.class, () -> cardService.topup(cardId, req));
    }

    // --- Spend: Rate limit exceeded ---
    @Test
    void spend_shouldThrow_whenRateLimitExceeded() {
        UUID cardId = UUID.randomUUID();
        CardRecord card = new CardRecord(cardId, "User", BigDecimal.valueOf(100), "ACTIVE", 0, LocalDateTime.now());
        SpendRequest req = new SpendRequest();
        req.setAmount(BigDecimal.valueOf(10));
        when(cardRepo.findById(cardId)).thenReturn(card);
        when(rateLimiter.allowSpend(cardId)).thenReturn(false);

        assertThrows(RateLimitExceededException.class, () -> cardService.spend(cardId, req));
    }
}
