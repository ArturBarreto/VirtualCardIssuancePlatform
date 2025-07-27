package com.nium.cardplatform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nium.cardplatform.dto.*;
import com.nium.cardplatform.exception.*;
import com.nium.cardplatform.service.CardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardController.class)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createCard_shouldReturnCreated_whenValid() throws Exception {
        CreateCardRequest req = new CreateCardRequest();
        req.setCardholderName("Alice");
        req.setInitialBalance(BigDecimal.valueOf(100));

        CardResponse resp = new CardResponse();
        resp.setId(UUID.randomUUID());
        resp.setCardholderName("Alice");
        resp.setBalance(BigDecimal.valueOf(100));
        resp.setStatus("ACTIVE");
        resp.setCreatedAt(LocalDateTime.now());

        when(cardService.createCard(any())).thenReturn(resp);

        mockMvc.perform(post("/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardholderName").value("Alice"))
                .andExpect(jsonPath("$.balance").value(100));
    }

    @Test
    void createCard_shouldReturnBadRequest_whenInvalid() throws Exception {
        CreateCardRequest req = new CreateCardRequest();
        req.setCardholderName("Bob");
        req.setInitialBalance(BigDecimal.valueOf(-10));

        doThrow(new InvalidTransactionAmountException("Initial balance must be greater or equal than zero."))
                .when(cardService).createCard(any());

        mockMvc.perform(post("/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Initial balance must be greater or equal than zero."));
    }

    @Test
    void spend_shouldReturnNoContent_whenValid() throws Exception {
        UUID cardId = UUID.randomUUID();
        SpendRequest req = new SpendRequest();
        req.setAmount(BigDecimal.valueOf(30));

        mockMvc.perform(post("/cards/" + cardId + "/spend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    @Test
    void spend_shouldReturnBadRequest_whenInvalidAmount() throws Exception {
        UUID cardId = UUID.randomUUID();
        SpendRequest req = new SpendRequest();
        req.setAmount(BigDecimal.ZERO);

        doThrow(new InvalidTransactionAmountException("Spend amount must be greater than zero."))
                .when(cardService).spend(eq(cardId), any());

        mockMvc.perform(post("/cards/" + cardId + "/spend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Spend amount must be greater than zero."));
    }

    @Test
    void spend_shouldReturnNotFound_whenCardNotFound() throws Exception {
        UUID cardId = UUID.randomUUID();
        SpendRequest req = new SpendRequest();
        req.setAmount(BigDecimal.valueOf(30));

        doThrow(new CardNotFoundException("Card not found: " + cardId))
                .when(cardService).spend(eq(cardId), any());

        mockMvc.perform(post("/cards/" + cardId + "/spend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Card not found: " + cardId));
    }

    @Test
    void spend_shouldReturnConflict_whenConcurrentModification() throws Exception {
        UUID cardId = UUID.randomUUID();
        SpendRequest req = new SpendRequest();
        req.setAmount(BigDecimal.valueOf(30));

        doThrow(new ConcurrentModificationException("Concurrent modification detected, try again."))
                .when(cardService).spend(eq(cardId), any());

        mockMvc.perform(post("/cards/" + cardId + "/spend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Concurrent modification detected, try again."));
    }

    @Test
    void spend_shouldReturnTooManyRequests_whenRateLimitExceeded() throws Exception {
        UUID cardId = UUID.randomUUID();
        SpendRequest req = new SpendRequest();
        req.setAmount(BigDecimal.valueOf(30));

        doThrow(new RateLimitExceededException("Max 5 spends per minute exceeded for card: " + cardId))
                .when(cardService).spend(eq(cardId), any());

        mockMvc.perform(post("/cards/" + cardId + "/spend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Max 5 spends per minute exceeded for card: " + cardId));
    }

    @Test
    void topup_shouldReturnNoContent_whenValid() throws Exception {
        UUID cardId = UUID.randomUUID();
        TopupRequest req = new TopupRequest();
        req.setAmount(BigDecimal.valueOf(50));

        mockMvc.perform(post("/cards/" + cardId + "/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    @Test
    void topup_shouldReturnBadRequest_whenInvalidAmount() throws Exception {
        UUID cardId = UUID.randomUUID();
        TopupRequest req = new TopupRequest();
        req.setAmount(BigDecimal.ZERO);

        doThrow(new InvalidTransactionAmountException("Top-up amount must be greater than zero."))
                .when(cardService).topup(eq(cardId), any());

        mockMvc.perform(post("/cards/" + cardId + "/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Top-up amount must be greater than zero."));
    }

    @Test
    void topup_shouldReturnNotFound_whenCardNotFound() throws Exception {
        UUID cardId = UUID.randomUUID();
        TopupRequest req = new TopupRequest();
        req.setAmount(BigDecimal.valueOf(10));

        doThrow(new CardNotFoundException("Card not found: " + cardId))
                .when(cardService).topup(eq(cardId), any());

        mockMvc.perform(post("/cards/" + cardId + "/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Card not found: " + cardId));
    }

    @Test
    void getCard_shouldReturnOk_whenCardExists() throws Exception {
        UUID cardId = UUID.randomUUID();
        CardResponse resp = new CardResponse();
        resp.setId(cardId);
        resp.setCardholderName("Alice");
        resp.setBalance(BigDecimal.valueOf(100));
        resp.setStatus("ACTIVE");
        resp.setCreatedAt(LocalDateTime.now());

        when(cardService.getCard(cardId)).thenReturn(resp);

        mockMvc.perform(get("/cards/" + cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardholderName").value("Alice"));
    }

    @Test
    void getCard_shouldReturnNotFound_whenCardNotFound() throws Exception {
        UUID cardId = UUID.randomUUID();
        when(cardService.getCard(cardId)).thenThrow(new CardNotFoundException("Card not found: " + cardId));

        mockMvc.perform(get("/cards/" + cardId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Card not found: " + cardId));
    }

    @Test
    void getTransactions_shouldReturnOk_whenTransactionsExist() throws Exception {
        UUID cardId = UUID.randomUUID();
        TransactionResponse tx = new TransactionResponse();
        tx.setId(UUID.randomUUID());
        tx.setCardId(cardId);
        tx.setType("SPEND");
        tx.setAmount(BigDecimal.valueOf(10));
        tx.setCreatedAt(LocalDateTime.now());

        when(cardService.getTransactions(cardId, 10, 0)).thenReturn(List.of(tx));

        mockMvc.perform(get("/cards/" + cardId + "/transactions")
                        .param("limit", "10")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("SPEND"));
    }

    @Test
    void getTransactions_shouldReturnNotFound_whenCardNotFound() throws Exception {
        UUID cardId = UUID.randomUUID();
        when(cardService.getTransactions(cardId, 10, 0))
                .thenThrow(new CardNotFoundException("Card not found: " + cardId));

        mockMvc.perform(get("/cards/" + cardId + "/transactions")
                        .param("limit", "10")
                        .param("offset", "0"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Card not found: " + cardId));
    }

    @Test
    void blockCard_shouldReturnNoContent_whenValid() throws Exception {
        UUID cardId = UUID.randomUUID();

        mockMvc.perform(post("/cards/" + cardId + "/block"))
                .andExpect(status().isNoContent());
    }

    @Test
    void blockCard_shouldReturnNotFound_whenCardNotFound() throws Exception {
        UUID cardId = UUID.randomUUID();

        doThrow(new CardNotFoundException("Card not found: " + cardId))
                .when(cardService).blockCard(cardId);

        mockMvc.perform(post("/cards/" + cardId + "/block"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Card not found: " + cardId));
    }

    @Test
    void unblockCard_shouldReturnNoContent_whenValid() throws Exception {
        UUID cardId = UUID.randomUUID();

        mockMvc.perform(post("/cards/" + cardId + "/unblock"))
                .andExpect(status().isNoContent());
    }

    @Test
    void unblockCard_shouldReturnNotFound_whenCardNotFound() throws Exception {
        UUID cardId = UUID.randomUUID();

        doThrow(new CardNotFoundException("Card not found: " + cardId))
                .when(cardService).unblockCard(cardId);

        mockMvc.perform(post("/cards/" + cardId + "/unblock"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Card not found: " + cardId));
    }
}
