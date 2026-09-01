package io.github.pnoker.db.r2dbc.runtime.transaction;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringR2dbcPageTransactionTest {

    @Test
    void usesAReadOnlyRepeatableReadSnapshot() {
        ReactiveTransactionManager transactionManager = mock(ReactiveTransactionManager.class);
        ReactiveTransaction transaction = mock(ReactiveTransaction.class);
        AtomicReference<TransactionDefinition> definition = new AtomicReference<>();
        when(transactionManager.getReactiveTransaction(any())).thenAnswer(invocation -> {
            definition.set(invocation.getArgument(0));
            return Mono.just(transaction);
        });
        when(transactionManager.commit(transaction)).thenReturn(Mono.empty());
        when(transactionManager.rollback(transaction)).thenReturn(Mono.empty());

        StepVerifier.create(new SpringR2dbcPageTransaction(transactionManager).transactional(Mono.just("page")))
                .expectNext("page")
                .verifyComplete();

        assertThat(definition.get()).isNotNull();
        assertThat(definition.get().isReadOnly()).isTrue();
        assertThat(definition.get().getIsolationLevel())
                .isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        assertThat(definition.get().getPropagationBehavior())
                .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
}
