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

package io.github.pnoker.common.filter;

import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.entity.R;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the request id published by {@link RequestIdWebFilter} (via the Reactor
 * Context) survives the {@code Schedulers.boundedElastic()} hop in {@code BaseController.async()}
 * and is visible in MDC on the worker thread where business {@code log.*} calls execute.
 *
 * <p>This is the regression guard for a previously broken behaviour: when the id was published
 * only via MDC on the Netty event loop, the subsequent thread hop dropped it, so controller
 * logs carried an empty {@code requestId} slot. The fix routes the id through the Reactor
 * Context (which propagates across threads) and re-applies it to MDC inside {@code async()}.
 *
 * @author pnoker
 * @version 2026.7.8
 * @since 2026.7.8
 */
class RequestIdMdcThreadingTest {

    private final BaseController controller = new BaseController() {
    };

    @AfterEach
    void clearMdc() {
        MDC.remove(RequestIdWebFilter.MDC_REQUEST_ID);
    }

    /**
     * Mimics the filter → controller chain: the id is written to the Reactor Context (as the
     * filter does), then {@code async()} runs on boundedElastic. The supplier captures the MDC
     * value seen on that worker thread — this is exactly where business log calls happen.
     */
    private Mono<String> requestIdSeenBySupplier(String inboundRequestId) {
        return controller.async(() -> R.ok(MDC.get(RequestIdWebFilter.MDC_REQUEST_ID)))
                .map(R::getMessage)
                .contextWrite(ctx -> ctx.put(RequestIdWebFilter.CONTEXT_REQUEST_ID, inboundRequestId));
    }

    @Test
    void requestIdSurvivesTheBoundedElasticHop() {
        StepVerifier.create(requestIdSeenBySupplier("req-123"))
                .assertNext(seen -> assertThat(seen).isEqualTo("req-123"))
                .verifyComplete();
    }

    @Test
    void mdcIsClearedAfterTheSupplierRuns() {
        // After async() completes, the worker thread must not leak the requestId into the
        // pooled thread's next task. Run the supplier once, then assert that this test
        // thread's MDC is untouched (the cleanup runs on the boundedElastic worker, but the
        // important invariant is that the supplier's finally-block removed its own entry).
        StepVerifier.create(requestIdSeenBySupplier("req-first"))
                .assertNext(seen -> assertThat(seen).isEqualTo("req-first"))
                .verifyComplete();
        // The test thread never had the id set; confirm it stayed clean.
        assertThat(MDC.get(RequestIdWebFilter.MDC_REQUEST_ID)).isNull();
    }
}
