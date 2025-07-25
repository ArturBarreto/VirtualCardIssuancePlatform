package com.nium.cardplatform.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class RateLimiterService {
    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MILLIS = 60 * 1000L;

    private final Map<UUID, Queue<Long>> cardSpendTimestamps = new ConcurrentHashMap<>();

    public synchronized boolean allowSpend(UUID cardId) {
        long now = Instant.now().toEpochMilli();
        Queue<Long> timestamps = cardSpendTimestamps.computeIfAbsent(cardId, k -> new ConcurrentLinkedQueue<>());

        // Remove timestamps older than the window
        while (!timestamps.isEmpty() && now - timestamps.peek() > WINDOW_MILLIS) {
            timestamps.poll();
        }

        if (timestamps.size() < MAX_REQUESTS) {
            timestamps.add(now);
            return true;
        } else {
            return false;
        }
    }
}
