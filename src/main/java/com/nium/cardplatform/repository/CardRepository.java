package com.nium.cardplatform.repository;

import com.nium.cardplatform.jooq.tables.records.CardRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

import static com.nium.cardplatform.jooq.tables.Card.CARD;

@Repository
public class CardRepository {
    private final DSLContext dsl;

    public CardRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    // Create a new card
    public void create(CardRecord card) {
        dsl.insertInto(CARD)
                .set(card)
                .execute();
    }

    // Find card by id
    public CardRecord findById(UUID id) {
        return dsl.selectFrom(CARD)
                .where(CARD.ID.eq(id))
                .fetchOne();
    }

    // Update balance and version (optimistic locking support)
    public int updateBalanceAndVersion(UUID id, BigDecimal newBalance, int expectedVersion) {
        return dsl.update(CARD)
                .set(CARD.BALANCE, newBalance)
                .set(CARD.VERSION, expectedVersion + 1)
                .where(CARD.ID.eq(id).and(CARD.VERSION.eq(expectedVersion)))
                .execute();
    }

    // Block card
    public int blockCard(UUID id) {
        return dsl.update(CARD)
                .set(CARD.STATUS, "BLOCKED")
                .where(CARD.ID.eq(id))
                .execute();
    }

    // Update card status (BLOCKED/ACTIVE)
    public int updateStatus(UUID id, String status) {
        return dsl.update(CARD)
                .set(CARD.STATUS, status)
                .where(CARD.ID.eq(id))
                .execute();
    }

    // List cards (for admin use)
    // public List<CardRecord> findAll() {...}
}
