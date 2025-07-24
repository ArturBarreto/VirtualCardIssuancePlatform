package com.nium.cardplatform.controller;

import com.nium.cardplatform.dto.*;
import com.nium.cardplatform.service.CardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/cards")
public class CardController {

    private final CardService service;

    public CardController(CardService service) {
        this.service = service;
    }

    // POST /cards - create a new virtual card
    @PostMapping
    public ResponseEntity<CardResponse> createCard(@RequestBody CreateCardRequest req) {
        CardResponse resp = service.createCard(req);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    // POST /cards/{id}/spend - spend from the card
    @PostMapping("/{id}/spend")
    public ResponseEntity<Void> spend(
            @PathVariable("id") UUID cardId,
            @RequestBody SpendRequest req
    ) {
        service.spend(cardId, req);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // POST /cards/{id}/topup - top up the card
    @PostMapping("/{id}/topup")
    public ResponseEntity<Void> topup(
            @PathVariable("id") UUID cardId,
            @RequestBody TopupRequest req
    ) {
        service.topup(cardId, req);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // GET /cards/{id} - get card details
    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCard(@PathVariable("id") UUID cardId) {
        CardResponse resp = service.getCard(cardId);
        return ResponseEntity.ok(resp);
    }

    // GET /cards/{id}/transactions - get card transaction history, with optional pagination
    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @PathVariable("id") UUID cardId,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset
    ) {
        List<TransactionResponse> txs = service.getTransactions(cardId, limit, offset);
        return ResponseEntity.ok(txs);
    }
}
