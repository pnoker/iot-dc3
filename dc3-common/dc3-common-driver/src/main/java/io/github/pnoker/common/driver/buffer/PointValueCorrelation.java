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

package io.github.pnoker.common.driver.buffer;

import lombok.Getter;
import org.springframework.amqp.rabbit.connection.CorrelationData;

/**
 * Carries the buffer record identity and serialized payload through the publisher-confirm
 * callback so a NACK can re-queue the point value without re-reading the message body.
 *
 * <p>{@code attempt} is the ordinal of the send attempt that just failed, so a NACK
 * republish stores the same counter the synchronous catch path would.
 *
 * @author pnoker
 * @since 2026.6.2
 */
@Getter
public class PointValueCorrelation extends CorrelationData {

    private final Long deviceId;
    private final Long pointId;
    private final int attempt;
    private final String payloadJson;
    private final String routingKey;

    public PointValueCorrelation(String id, Long deviceId, Long pointId, int attempt,
                                 String payloadJson, String routingKey) {
        super(id);
        this.deviceId = deviceId;
        this.pointId = pointId;
        this.attempt = attempt;
        this.payloadJson = payloadJson;
        this.routingKey = routingKey;
    }
}
