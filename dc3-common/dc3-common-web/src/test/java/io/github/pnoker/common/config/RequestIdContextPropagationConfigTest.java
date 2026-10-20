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
package io.github.pnoker.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pnoker.common.constant.common.RequestIdConstant;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Verifies the request id flows from the Reactor context into the MDC on
 * whatever thread the chain hops to, closing the trace gap where the
 * {@code [%X{requestId:-}]} log slot stayed empty inside reactive operators.
 */
class RequestIdContextPropagationConfigTest {

    @Test
    void requestIdFlowsFromReactorContextIntoMdcAcrossThreadHops() {
        new RequestIdContextPropagationConfig().register();

        String seen = Mono.fromCallable(() -> MDC.get(RequestIdConstant.MDC_KEY))
                .subscribeOn(Schedulers.boundedElastic())
                .contextWrite(ctx -> ctx.put(RequestIdConstant.REACTOR_CONTEXT_KEY, "req-42"))
                .block(Duration.ofSeconds(10));

        assertThat(seen).isEqualTo("req-42");
    }

    @Test
    void registrationIsIdempotentAcrossInstances() {
        RequestIdContextPropagationConfig first = new RequestIdContextPropagationConfig();
        first.register();
        first.register();
        new RequestIdContextPropagationConfig().register();
    }
}
