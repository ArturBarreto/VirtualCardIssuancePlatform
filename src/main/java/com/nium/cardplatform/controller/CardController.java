package com.nium.cardplatform.controller;

import com.nium.cardplatform.dto.*;
import com.nium.cardplatform.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(
        name = "Cards",
        description = "APIs for creating, managing, blocking, and transacting with virtual cards."
)
@RestController
@RequestMapping("/cards")
public class CardController {

    private final CardService service;

    public CardController(CardService service) {
        this.service = service;
    }

    @Operation(
            summary = "Create a new virtual card",
            description = "Creates a virtual card for a cardholder with the given initial balance (must be zero or positive)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Card created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CardResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request (e.g., negative initial balance)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    value = "{ \"timestamp\": \"2025-07-24T17:45:31.123\", \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Initial balance must be greater or equal than zero.\" }"
                            )
                    )
            )
    })
    @PostMapping
    public ResponseEntity<CardResponse> createCard(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Cardholder name and initial balance",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateCardRequest.class),
                            examples = @ExampleObject(value = "{ \"cardholderName\": \"Alice\", \"initialBalance\": 100.0 }")
                    )
            )
            @RequestBody CreateCardRequest req
    ) {
        CardResponse resp = service.createCard(req);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Spend from card",
            description = "Deducts an amount from the card if sufficient balance, card is active, and rate limit is not exceeded."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Spend successful"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid amount or business rule error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    value = "{ \"timestamp\": \"2025-07-24T17:45:31.123\", \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Spend amount must be greater than zero.\" }"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Card not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    value = "{ \"timestamp\": \"2025-07-24T17:45:31.123\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"Card not found: d290f1ee-6c54-4b01-90e6-d701748f0851\" }"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Optimistic concurrency conflict",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    value = "{ \"timestamp\": \"2025-07-24T17:45:31.123\", \"status\": 409, \"error\": \"Conflict\", \"message\": \"Concurrent modification detected, try again.\" }"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    value = "{ \"timestamp\": \"2025-07-24T17:45:31.123\", \"status\": 429, \"error\": \"Too Many Requests\", \"message\": \"Max 5 spends per minute exceeded for card: d290f1ee-6c54-4b01-90e6-d701748f0851\" }"
                            )
                    )
            )
    })
    @PostMapping("/{id}/spend")
    public ResponseEntity<Void> spend(
            @Parameter(description = "UUID of the card", required = true, example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
            @PathVariable("id") UUID cardId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Amount to spend (must be > 0)",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = SpendRequest.class),
                            examples = @ExampleObject(value = "{ \"amount\": 30.0 }")
                    )
            )
            @RequestBody SpendRequest req
    ) {
        service.spend(cardId, req);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(
            summary = "Top up card",
            description = "Adds funds to the card balance (amount must be greater than zero)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Top-up successful"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid amount",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    value = "{ \"timestamp\": \"2025-07-24T17:45:31.123\", \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Top-up amount must be greater than zero.\" }"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Card not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    value = "{ \"timestamp\": \"2025-07-24T17:45:31.123\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"Card not found: d290f1ee-6c54-4b01-90e6-d701748f0851\" }"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Optimistic concurrency conflict",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    value = "{ \"timestamp\": \"2025-07-24T17:45:31.123\", \"status\": 409, \"error\": \"Conflict\", \"message\": \"Concurrent modification detected, try again.\" }"
                            )
                    )
            )
    })
    @PostMapping("/{id}/topup")
    public ResponseEntity<Void> topup(
            @Parameter(description = "UUID of the card", required = true, example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
            @PathVariable("id") UUID cardId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Amount to top up (must be > 0)",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = TopupRequest.class),
                            examples = @ExampleObject(value = "{ \"amount\": 50.0 }")
                    )
            )
            @RequestBody TopupRequest req
    ) {
        service.topup(cardId, req);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(
            summary = "Get card details",
            description = "Retrieves information about the card, including balance, status, and creation date."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Card found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CardResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Card not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    value = "{ \"timestamp\": \"2025-07-24T17:45:31.123\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"Card not found: d290f1ee-6c54-4b01-90e6-d701748f0851\" }"
                            )
                    )
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCard(
            @Parameter(description = "UUID of the card", required = true, example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
            @PathVariable("id") UUID cardId
    ) {
        CardResponse resp = service.getCard(cardId);
        return ResponseEntity.ok(resp);
    }

    @Operation(
            summary = "List card transactions",
            description = "Returns a paginated list of transactions for the specified card."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transaction list returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Card not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    value = "{ \"timestamp\": \"2025-07-24T17:45:31.123\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"Card not found: d290f1ee-6c54-4b01-90e6-d701748f0851\" }"
                            )
                    )
            )
    })
    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @Parameter(description = "UUID of the card", required = true, example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
            @PathVariable("id") UUID cardId,
            @Parameter(description = "Page limit", example = "10")
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(name = "offset", defaultValue = "0") int offset
    ) {
        List<TransactionResponse> txs = service.getTransactions(cardId, limit, offset);
        return ResponseEntity.ok(txs);
    }

    @Operation(
            summary = "Block a card",
            description = "Blocks the card. Blocked cards cannot perform spend or top-up operations."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Card blocked"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Card not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    value = "{ \"timestamp\": \"2025-07-24T17:45:31.123\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"Card not found: d290f1ee-6c54-4b01-90e6-d701748f0851\" }"
                            )
                    )
            )
    })
    @PostMapping("/{id}/block")
    public ResponseEntity<Void> blockCard(
            @Parameter(description = "UUID of the card", required = true, example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
            @PathVariable("id") UUID cardId
    ) {
        service.blockCard(cardId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Unblock a card",
            description = "Unblocks the card, setting its status back to ACTIVE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Card unblocked"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Card not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    value = "{ \"timestamp\": \"2025-07-24T17:45:31.123\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"Card not found: d290f1ee-6c54-4b01-90e6-d701748f0851\" }"
                            )
                    )
            )
    })
    @PostMapping("/{id}/unblock")
    public ResponseEntity<Void> unblockCard(
            @Parameter(description = "UUID of the card", required = true, example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
            @PathVariable("id") UUID cardId
    ) {
        service.unblockCard(cardId);
        return ResponseEntity.noContent().build();
    }
}
