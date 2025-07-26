package com.nium.cardplatform;

import com.nium.cardplatform.dto.CardResponse;
import com.nium.cardplatform.dto.CreateCardRequest;
import com.nium.cardplatform.dto.SpendRequest;
import com.nium.cardplatform.dto.TopupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CardPlatformEndToEndTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void fullCardLifecycle() {
        // 1. Create a card
        CreateCardRequest createReq = new CreateCardRequest();
        createReq.setCardholderName("E2E User");
        createReq.setInitialBalance(BigDecimal.valueOf(150.00));

        ResponseEntity<CardResponse> createResp = restTemplate.postForEntity(
                url("/cards"), createReq, CardResponse.class);

        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResp.getBody()).isNotNull();
        UUID cardId = createResp.getBody().getId();

        // 2. Get card info
        ResponseEntity<CardResponse> getResp = restTemplate.getForEntity(
                url("/cards/" + cardId), CardResponse.class);

        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResp.getBody().getBalance()).isEqualByComparingTo("150.00");

        // 3. Top up
        HttpEntity<?> topupEntity = new HttpEntity<>(new com.nium.cardplatform.dto.TopupRequest(BigDecimal.valueOf(50)));
        ResponseEntity<Void> topupResp = restTemplate.postForEntity(
                url("/cards/" + cardId + "/topup"), topupEntity, Void.class);
        assertThat(topupResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 4. Spend
        HttpEntity<?> spendEntity = new HttpEntity<>(new com.nium.cardplatform.dto.SpendRequest(BigDecimal.valueOf(30)));
        ResponseEntity<Void> spendResp = restTemplate.postForEntity(
                url("/cards/" + cardId + "/spend"), spendEntity, Void.class);
        assertThat(spendResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 5. Get card again and check updated balance
        ResponseEntity<CardResponse> afterResp = restTemplate.getForEntity(
                url("/cards/" + cardId), CardResponse.class);
        assertThat(afterResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(afterResp.getBody().getBalance()).isEqualByComparingTo("170.00");

        // 6. Get transactions
        ResponseEntity<String> txListResp = restTemplate.getForEntity(
                url("/cards/" + cardId + "/transactions"), String.class);
        assertThat(txListResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(txListResp.getBody()).contains("TOPUP").contains("SPEND");

        // 7. Block the card
        ResponseEntity<Void> blockResp = restTemplate.postForEntity(
                url("/cards/" + cardId + "/block"), null, Void.class);
        assertThat(blockResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 8. Try to spend on blocked card (should fail)
        spendResp = restTemplate.postForEntity(
                url("/cards/" + cardId + "/spend"), spendEntity, Void.class);
        assertThat(spendResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST); // or whatever you mapped

        // 9. Unblock the card
        ResponseEntity<Void> unblockResp = restTemplate.postForEntity(
                url("/cards/" + cardId + "/unblock"), null, Void.class);
        assertThat(unblockResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 10. Spend should now succeed again
        spendResp = restTemplate.postForEntity(
                url("/cards/" + cardId + "/spend"), spendEntity, Void.class);
        assertThat(spendResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void spend_shouldFail_whenInsufficientBalance() {
        // 1. Create card with 10
        CreateCardRequest req = new CreateCardRequest();
        req.setCardholderName("Insufficient");
        req.setInitialBalance(BigDecimal.valueOf(10));
        ResponseEntity<CardResponse> createResp = restTemplate.postForEntity(
                url("/cards"), req, CardResponse.class);

        UUID cardId = createResp.getBody().getId();

        // 2. Try to spend 20
        com.nium.cardplatform.dto.SpendRequest spendReq = new com.nium.cardplatform.dto.SpendRequest();
        spendReq.setAmount(BigDecimal.valueOf(20));
        HttpEntity<?> spendEntity = new HttpEntity<>(spendReq);

        ResponseEntity<String> spendResp = restTemplate.postForEntity(
                url("/cards/" + cardId + "/spend"), spendEntity, String.class);

        assertThat(spendResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(spendResp.getBody()).contains("Insufficient balance");
    }

    @Test
    void spend_shouldReturnNotFound_whenCardDoesNotExist() {
        UUID fakeId = UUID.randomUUID();
        com.nium.cardplatform.dto.SpendRequest spendReq = new com.nium.cardplatform.dto.SpendRequest();
        spendReq.setAmount(BigDecimal.valueOf(10));
        HttpEntity<?> spendEntity = new HttpEntity<>(spendReq);

        ResponseEntity<String> spendResp = restTemplate.postForEntity(
                url("/cards/" + fakeId + "/spend"), spendEntity, String.class);

        assertThat(spendResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void spend_shouldReturnTooManyRequests_whenRateLimitExceeded() {
        // 1. Create card
        CreateCardRequest req = new CreateCardRequest();
        req.setCardholderName("RateLimit");
        req.setInitialBalance(BigDecimal.valueOf(100));
        ResponseEntity<CardResponse> createResp = restTemplate.postForEntity(
                url("/cards"), req, CardResponse.class);

        UUID cardId = createResp.getBody().getId();

        com.nium.cardplatform.dto.SpendRequest spendReq = new com.nium.cardplatform.dto.SpendRequest();
        spendReq.setAmount(BigDecimal.valueOf(1));
        HttpEntity<?> spendEntity = new HttpEntity<>(spendReq);

        // Make 5 successful spends
        for (int i = 0; i < 5; i++) {
            ResponseEntity<Void> spendResp = restTemplate.postForEntity(
                    url("/cards/" + cardId + "/spend"), spendEntity, Void.class);
            assertThat(spendResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
        // 6th should be rate limited
        ResponseEntity<String> spendResp = restTemplate.postForEntity(
                url("/cards/" + cardId + "/spend"), spendEntity, String.class);
        assertThat(spendResp.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void getCard_shouldReturnNotFound_whenDoesNotExist() {
        UUID fakeId = UUID.randomUUID();
        ResponseEntity<String> resp = restTemplate.getForEntity(url("/cards/" + fakeId), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getCard_shouldReturnBadRequest_whenInvalidUUID() {
        ResponseEntity<String> resp = restTemplate.getForEntity(url("/cards/not-a-uuid"), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createCard_shouldReturnBadRequest_whenBodyMissing() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("", headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(url("/cards"), entity, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void blockCard_shouldReturnNoContent_whenAlreadyBlocked() {
        // Create and block
        CreateCardRequest req = new CreateCardRequest();
        req.setCardholderName("BlockTest");
        req.setInitialBalance(BigDecimal.TEN);
        UUID cardId = restTemplate.postForEntity(url("/cards"), req, CardResponse.class).getBody().getId();
        restTemplate.postForEntity(url("/cards/" + cardId + "/block"), null, Void.class);

        // Block again
        ResponseEntity<Void> resp = restTemplate.postForEntity(url("/cards/" + cardId + "/block"), null, Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void getTransactions_shouldReturnEmptyList_whenNoTransactions() {
        CreateCardRequest req = new CreateCardRequest();
        req.setCardholderName("TxPagination");
        req.setInitialBalance(BigDecimal.ZERO);
        UUID cardId = restTemplate.postForEntity(url("/cards"), req, CardResponse.class).getBody().getId();

        ResponseEntity<String> resp = restTemplate.getForEntity(url("/cards/" + cardId + "/transactions?limit=10&offset=0"), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("[]"); // or check for empty array
    }

    @Test
    void shouldReturnMethodNotAllowed_whenPostToGetEndpoint() {
        CreateCardRequest req = new CreateCardRequest();
        req.setCardholderName("MethodTest");
        req.setInitialBalance(BigDecimal.TEN);

        ResponseEntity<String> resp = restTemplate.postForEntity(url("/cards/" + UUID.randomUUID()), req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void concurrentSpends_shouldPreventDoubleSpend() throws Exception {
        // 1. Create card with 50
        CreateCardRequest req = new CreateCardRequest();
        req.setCardholderName("Concurrent");
        req.setInitialBalance(BigDecimal.valueOf(50));
        UUID cardId = restTemplate.postForEntity(url("/cards"), req, CardResponse.class).getBody().getId();

        // 2. Define spend of 40 (twice)
        Runnable spendTask = () -> {
            SpendRequest spendReq = new SpendRequest();
            spendReq.setAmount(BigDecimal.valueOf(40));
            HttpEntity<SpendRequest> entity = new HttpEntity<>(spendReq);
            restTemplate.postForEntity(url("/cards/" + cardId + "/spend"), entity, String.class);
        };

        // 3. Run 2 threads nearly simultaneously
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> f1 = executor.submit(spendTask);
        Future<?> f2 = executor.submit(spendTask);
        f1.get();
        f2.get();
        executor.shutdown();

        // 4. Check final balance is 10 (not negative) and only one spend succeeded
        CardResponse resp = restTemplate.getForEntity(url("/cards/" + cardId), CardResponse.class).getBody();
        assertThat(resp.getBalance()).isEqualByComparingTo("10");
    }

    @Test
    void multiThreadedTopupAndSpend_shouldStayConsistent() throws Exception {
        CreateCardRequest req = new CreateCardRequest();
        req.setCardholderName("MultiThreaded");
        req.setInitialBalance(BigDecimal.valueOf(100));
        UUID cardId = restTemplate.postForEntity(url("/cards"), req, CardResponse.class).getBody().getId();

        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads * 2);

        Runnable topupTask = () -> {
            TopupRequest tReq = new TopupRequest();
            tReq.setAmount(BigDecimal.valueOf(10));
            do {
                ResponseEntity<String> resp = restTemplate.postForEntity(
                        url("/cards/" + cardId + "/topup"),
                        new HttpEntity<>(tReq),
                        String.class);
                if (resp.getStatusCode() == HttpStatus.NO_CONTENT) break;
            } while (true);
        };
        Runnable spendTask = () -> {
            SpendRequest sReq = new SpendRequest();
            sReq.setAmount(BigDecimal.valueOf(10));
            do {
                ResponseEntity<String> resp = restTemplate.postForEntity(
                        url("/cards/" + cardId + "/spend"),
                        new HttpEntity<>(sReq),
                        String.class);
                if (resp.getStatusCode() == HttpStatus.NO_CONTENT) break;
            } while (true);
        };

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(Executors.callable(topupTask, null));
            tasks.add(Executors.callable(spendTask, null));
        }
        executor.invokeAll(tasks);
        executor.shutdown();

        // The balance should remain as initial, since equal topups/spends
        CardResponse resp = restTemplate.getForEntity(url("/cards/" + cardId), CardResponse.class).getBody();
        assertThat(resp.getBalance()).isEqualByComparingTo("100");
    }

    @Test
    void spend_shouldBeRateLimitedUnderHighLoad() {
        CreateCardRequest req = new CreateCardRequest();
        req.setCardholderName("FloodTest");
        req.setInitialBalance(BigDecimal.valueOf(100));
        UUID cardId = restTemplate.postForEntity(url("/cards"), req, CardResponse.class).getBody().getId();

        SpendRequest spendReq = new SpendRequest();
        spendReq.setAmount(BigDecimal.valueOf(1));
        HttpEntity<SpendRequest> entity = new HttpEntity<>(spendReq);

        int allowed = 0, rateLimited = 0;
        for (int i = 0; i < 10; i++) {
            ResponseEntity<String> resp = restTemplate.postForEntity(url("/cards/" + cardId + "/spend"), entity, String.class);
            if (resp.getStatusCode() == HttpStatus.NO_CONTENT) allowed++;
            if (resp.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) rateLimited++;
        }
        assertThat(allowed).isEqualTo(5); // Max 5 spends per minute
        assertThat(rateLimited).isGreaterThan(0);
    }

    @Test
    void shouldReturn400_whenUUIDIsInvalid() {
        SpendRequest spendReq = new SpendRequest();
        spendReq.setAmount(BigDecimal.TEN);
        HttpEntity<SpendRequest> entity = new HttpEntity<>(spendReq);

        ResponseEntity<String> resp = restTemplate.postForEntity(url("/cards/not-a-uuid/spend"), entity, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void blockedCard_shouldNotAllowSpendOrTopup_evenConcurrently() throws Exception {
        CreateCardRequest req = new CreateCardRequest();
        req.setCardholderName("BlockConcurrent");
        req.setInitialBalance(BigDecimal.valueOf(20));
        UUID cardId = restTemplate.postForEntity(url("/cards"), req, CardResponse.class).getBody().getId();

        // Block the card
        restTemplate.postForEntity(url("/cards/" + cardId + "/block"), null, Void.class);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Runnable spendTask = () -> {
            SpendRequest sReq = new SpendRequest();
            sReq.setAmount(BigDecimal.TEN);
            HttpEntity<SpendRequest> entity = new HttpEntity<>(sReq);
            ResponseEntity<String> resp = restTemplate.postForEntity(url("/cards/" + cardId + "/spend"), entity, String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        };
        Runnable topupTask = () -> {
            TopupRequest tReq = new TopupRequest();
            tReq.setAmount(BigDecimal.TEN);
            HttpEntity<TopupRequest> entity = new HttpEntity<>(tReq);
            ResponseEntity<String> resp = restTemplate.postForEntity(url("/cards/" + cardId + "/topup"), entity, String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        };
        executor.invokeAll(List.of(Executors.callable(spendTask), Executors.callable(topupTask)));
        executor.shutdown();
    }

    @Test
    void blockAndUnblockMultipleTimes_shouldAlwaysReturnNoContent() {
        CreateCardRequest req = new CreateCardRequest();
        req.setCardholderName("Reentrant");
        req.setInitialBalance(BigDecimal.TEN);
        UUID cardId = restTemplate.postForEntity(url("/cards"), req, CardResponse.class).getBody().getId();

        for (int i = 0; i < 3; i++) {
            assertThat(restTemplate.postForEntity(url("/cards/" + cardId + "/block"), null, Void.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(restTemplate.postForEntity(url("/cards/" + cardId + "/unblock"), null, Void.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }



}
