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
package io.github.pnoker.common.agentic.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.agentic.entity.bo.MessageBO;
import io.github.pnoker.common.agentic.repository.ReactiveMessageStore;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.RequestException;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * Verifies the message service pagination contract: chronological slicing, totals,
 * hasNext, and the platform bounds (limit 1..200, non-negative offset).
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private ReactiveMessageStore messageStore;

    private MessageServiceImpl service;

    private RequestHeader.PrincipalHeader header;

    @BeforeEach
    void setUp() {
        service = new MessageServiceImpl(messageStore);
        header = new RequestHeader.PrincipalHeader();
        header.setTenantId(1L);
        header.setPrincipalId(2L);
        header.setPrincipalName("admin");
    }

    @Test
    void slicesTheSecondPageWithTotalsAndHasNext() {
        when(messageStore.list("conv-1", header)).thenReturn(Flux.fromIterable(messages(5)));

        StepVerifier.create(service.list(2L, 2, "conv-1", header))
                .assertNext(page -> {
                    assertThat(page.items()).hasSize(2);
                    assertThat(page.offset()).isEqualTo(2L);
                    assertThat(page.limit()).isEqualTo(2);
                    assertThat(page.total()).isEqualTo(5);
                    assertThat(page.hasNext()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void reportsNoNextPageWhenTheSliceReachesTheEnd() {
        when(messageStore.list("conv-1", header)).thenReturn(Flux.fromIterable(messages(5)));

        StepVerifier.create(service.list(4L, 2, "conv-1", header))
                .assertNext(page -> {
                    assertThat(page.items()).hasSize(1);
                    assertThat(page.hasNext()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void returnsEmptyPageBeyondTheEnd() {
        when(messageStore.list("conv-1", header)).thenReturn(Flux.fromIterable(messages(3)));

        StepVerifier.create(service.list(100L, 50, "conv-1", header))
                .assertNext(page -> {
                    assertThat(page.items()).isEmpty();
                    assertThat(page.total()).isEqualTo(3);
                    assertThat(page.hasNext()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void rejectsInvalidPagingBeforeTouchingTheStore() {
        StepVerifier.create(service.list(-1L, 50, "conv-1", header))
                .expectError(RequestException.class)
                .verify();
        StepVerifier.create(service.list(0L, 0, "conv-1", header))
                .expectError(RequestException.class)
                .verify();
        StepVerifier.create(service.list(0L, 201, "conv-1", header))
                .expectError(RequestException.class)
                .verify();
        StepVerifier.create(service.list(0L, 50, " ", header))
                .expectError(RequestException.class)
                .verify();
        verifyNoInteractions(messageStore);
    }

    private List<MessageBO> messages(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> {
                    MessageBO message = new MessageBO();
                    message.setConversationId("conv-1");
                    message.setRole("user");
                    message.setMessageIndex((long) index);
                    return message;
                })
                .toList();
    }
}
