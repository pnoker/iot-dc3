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

package io.github.pnoker.common.mq.core;

import io.github.pnoker.common.constant.common.RequestIdConstant;
import io.micrometer.context.ContextRegistry;
import org.slf4j.MDC;
import reactor.core.publisher.Hooks;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Registers the request-id MDC field as Reactor context state. */
final class MqContextPropagation {

    static final String REQUEST_ID_CONTEXT_KEY = "dc3.mq.request-id";
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private MqContextPropagation() {
        throw new IllegalStateException("Utility class");
    }

    static void initialize() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        ContextRegistry registry = ContextRegistry.getInstance();
        boolean registered = registry.getThreadLocalAccessors().stream()
                .anyMatch(accessor -> Objects.equals(accessor.key(), REQUEST_ID_CONTEXT_KEY));
        if (!registered) {
            registry.registerThreadLocalAccessor(REQUEST_ID_CONTEXT_KEY,
                    () -> MDC.get(RequestIdConstant.MDC_KEY),
                    value -> MDC.put(RequestIdConstant.MDC_KEY, value),
                    () -> MDC.remove(RequestIdConstant.MDC_KEY));
        }
        Hooks.enableAutomaticContextPropagation();
    }
}
