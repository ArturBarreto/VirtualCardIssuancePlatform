package com.nium.cardplatform.repository;

import com.nium.cardplatform.jooq.tables.records.TransactionRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static com.nium.cardplatform.jooq.tables.Transaction.TRANSACTION;

@Repository
public class TransactionRepository {
    private final DSLContext dsl;

    public TransactionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    // Create a transaction record
    public void create(TransactionRecord transaction) {
        dsl.insertInto(TRANSACTION)
                .set(transaction)
                .execute();
    }

    // Get all transactions for a card (optional: with pagination)
    public List<TransactionRecord> findByCardId(UUID cardId, int limit, int offset) {
        return dsl.selectFrom(TRANSACTION)
                .where(TRANSACTION.CARD_ID.eq(cardId))
                .orderBy(TRANSACTION.CREATED_AT.desc())
                .limit(limit)
                .offset(offset)
                .fetch();
    }

    // Count for pagination
    public int countByCardId(UUID cardId) {
        return dsl.fetchCount(
                dsl.selectFrom(TRANSACTION)
                        .where(TRANSACTION.CARD_ID.eq(cardId))
        );
    }
}
