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
package io.github.pnoker.common.agentic.service.direct;

/**
 * Backend data prepared before the model call.
 * <p>
 * {@code context} is appended to the model prompt for soft, non-deterministic flows.
 * {@code answer} bypasses the model and is rendered by {@link DirectAnswerRenderer}
 * when a deterministic platform query can be fully resolved by the backend.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
public record DirectBackendResult(String context, DirectAnswer answer) {

    public static DirectBackendResult contextOnly(String context) {
        return new DirectBackendResult(context, null);
    }

    public static DirectBackendResult direct(DirectAnswer answer) {
        return new DirectBackendResult(null, answer);
    }

}
