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
package io.github.pnoker.common.agentic.entity.model;

import io.github.pnoker.common.enums.ConnectivityErrorTypeEnum;
import io.github.pnoker.common.enums.ConnectivityStatusEnum;
import java.util.List;

/**
 * Internal outcome of one connectivity probe level (L1 or L2).
 *
 * @author pnoker
 * @since 2016.10.1
 */
public record ConnectivityLevelResult(
        ConnectivityStatusEnum status,
        long latencyMs,
        ConnectivityErrorTypeEnum errorType,
        String message,
        Integer upstreamStatus,
        List<String> models,
        String model) {

    /**
     * Successful L1 probe outcome.
     *
     * @param latencyMs round-trip latency in milliseconds
     * @param models    model ids the endpoint reported
     * @return pass result
     */
    public static ConnectivityLevelResult passL1(long latencyMs, List<String> models) {
        return new ConnectivityLevelResult(ConnectivityStatusEnum.PASS, latencyMs, null, null, null, models, null);
    }

    /**
     * Successful L2 probe outcome.
     *
     * @param latencyMs round-trip latency in milliseconds
     * @param model     model identifier the probe used
     * @return pass result
     */
    public static ConnectivityLevelResult passL2(long latencyMs, String model) {
        return new ConnectivityLevelResult(ConnectivityStatusEnum.PASS, latencyMs, null, null, null, null, model);
    }

    /**
     * Failed probe outcome.
     *
     * @param latencyMs      time spent before the failure, in milliseconds
     * @param errorType      classified failure cause
     * @param message        sanitized upstream error message
     * @param upstreamStatus upstream HTTP status when one was received
     * @return fail result
     */
    public static ConnectivityLevelResult fail(
            long latencyMs, ConnectivityErrorTypeEnum errorType, String message, Integer upstreamStatus) {
        return new ConnectivityLevelResult(
                ConnectivityStatusEnum.FAIL, latencyMs, errorType, message, upstreamStatus, null, null);
    }
}
