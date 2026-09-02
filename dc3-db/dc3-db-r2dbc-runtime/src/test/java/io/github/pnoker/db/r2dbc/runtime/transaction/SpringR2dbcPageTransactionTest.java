/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.pnoker.db.r2dbc.runtime.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
        assertThat(definition.get().getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        assertThat(definition.get().getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
}
