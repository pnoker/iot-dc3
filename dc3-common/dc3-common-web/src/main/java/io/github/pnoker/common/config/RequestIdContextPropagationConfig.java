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

import io.github.pnoker.common.constant.common.RequestIdConstant;
import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import reactor.core.publisher.Hooks;

/**
 * Bridges the request id between the Reactor context and SLF4J MDC so the
 * {@code [%X{requestId:-}]} log-pattern slot is populated on every thread a
 * reactive chain hops to — not only on the thread that entered the
 * {@code RequestIdWebFilter}.
 *
 * <p>Mirrors the MQ module's {@code MqContextPropagation} precedent: a
 * {@code ThreadLocalAccessor} is registered for the
 * {@code dc3.requestId} context key against the {@code requestId} MDC key,
 * and automatic context propagation is enabled globally. Hosts that also
 * carry the MQ module share the same global toggle; registering twice is
 * guarded and idempotent.
 */
@AutoConfiguration
public class RequestIdContextPropagationConfig {

    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    @PostConstruct
    void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        ContextRegistry registry = ContextRegistry.getInstance();
        boolean alreadyPresent = registry.getThreadLocalAccessors().stream()
                .anyMatch(accessor -> Objects.equals(accessor.key(), RequestIdConstant.REACTOR_CONTEXT_KEY));
        if (!alreadyPresent) {
            registry.registerThreadLocalAccessor(
                    RequestIdConstant.REACTOR_CONTEXT_KEY,
                    () -> MDC.get(RequestIdConstant.MDC_KEY),
                    value -> MDC.put(RequestIdConstant.MDC_KEY, value),
                    () -> MDC.remove(RequestIdConstant.MDC_KEY));
        }
        Hooks.enableAutomaticContextPropagation();
    }
}
