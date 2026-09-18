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
package io.github.pnoker.common.mq.adapter;

import io.github.pnoker.common.constant.mq.DeliveryDisposition;
import java.util.Map;

/**
 * Raw wire-level delivery an adapter hands to the core bridge: JSON body, string
 * headers and redelivery flag. Broker acknowledgment handles never cross the adapter
 * boundary; the listener returns a {@link DeliveryDisposition} after completion.
 *
 * @author pnoker
 * @since 2026.8.19
 */
public record WireMqDelivery(byte[] body, Map<String, String> headers, boolean redelivered) {}
