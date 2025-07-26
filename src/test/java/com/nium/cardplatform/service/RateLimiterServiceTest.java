package com.nium.cardplatform.service;

import com.nium.cardplatform.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiter;
    private UUID cardId;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiterService(1000L); // 1 second window for fast tests
        cardId = UUID.randomUUID();
    }

    @Test
    void allowSpend_allowsUpToFiveRequestsWithinWindow() {
        // First 5 spends should be allowed
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.allowSpend(cardId), "Spend " + (i+1) + " should be allowed");
        }
        // 6th spend should be blocked
        assertFalse(rateLimiter.allowSpend(cardId), "6th spend should be blocked");
    }

    @Test
    void allowSpend_allowsAgainAfterWindowExpires() throws InterruptedException {
        // Fill up 5 spends
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.allowSpend(cardId));
        }
        // Wait for window to pass
        Thread.sleep(1100); // (for demo, should be 61_000ms in production; here you can lower WINDOW_MILLIS to test faster)

        // Simulate clearing of timestamps
        assertTrue(rateLimiter.allowSpend(cardId));
    }

    @Test
    void allowSpend_isIsolatedPerCard() {
        UUID cardA = UUID.randomUUID();
        UUID cardB = UUID.randomUUID();
        // Each card gets 5 spends
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.allowSpend(cardA), "CardA spend " + (i+1));
            assertTrue(rateLimiter.allowSpend(cardB), "CardB spend " + (i+1));
        }
        // 6th for both is blocked
        assertFalse(rateLimiter.allowSpend(cardA));
        assertFalse(rateLimiter.allowSpend(cardB));
    }
}
