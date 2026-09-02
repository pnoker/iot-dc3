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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.repository.ReactiveSessionStore;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private ReactiveSessionStore sessionStore;

    @Mock
    private RequestHeader.PrincipalHeader header;

    private SessionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SessionServiceImpl(sessionStore);
    }

    @Test
    void delegatesTouchWithoutBlocking() {
        SessionBO session = new SessionBO();
        when(sessionStore.touch("conv", null, header)).thenReturn(Mono.just(session));

        StepVerifier.create(service.touch("conv", header, null))
                .expectNext(session)
                .verifyComplete();

        verify(sessionStore).touch("conv", null, header);
    }

    @Test
    void delegatesOffsetPage() {
        OffsetPage<SessionBO> page = OffsetPage.of(List.of(new SessionBO()), 0, 10, 1);
        when(sessionStore.list(0, 10, null, null, header)).thenReturn(Mono.just(page));

        StepVerifier.create(service.list(0, 10, null, null, header))
                .expectNext(page)
                .verifyComplete();
    }
}
