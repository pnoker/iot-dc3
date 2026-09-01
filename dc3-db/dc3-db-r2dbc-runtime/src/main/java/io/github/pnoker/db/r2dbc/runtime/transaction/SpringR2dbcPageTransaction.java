package io.github.pnoker.db.r2dbc.runtime.transaction;

import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Spring R2DBC implementation backed by a read-only repeatable-read transaction. */
public final class SpringR2dbcPageTransaction implements PageTransaction {

    private final TransactionalOperator transactionalOperator;

    public SpringR2dbcPageTransaction(ReactiveTransactionManager transactionManager) {
        Objects.requireNonNull(transactionManager, "transactionManager must not be null");
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setReadOnly(true);
        definition.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transactionalOperator = TransactionalOperator.create(transactionManager, definition);
    }

    @Override
    public <T> Mono<T> transactional(Mono<T> work) {
        return transactionalOperator.transactional(Objects.requireNonNull(work, "work must not be null"));
    }
}
