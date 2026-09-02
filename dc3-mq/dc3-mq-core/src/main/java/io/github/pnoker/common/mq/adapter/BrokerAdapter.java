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

import io.github.pnoker.common.mq.message.WireMqMessage;
import io.github.pnoker.common.mq.subscription.SubscriptionSpec;

/**
 * SPI implemented by each broker adapter module. Exactly one adapter is active at
 * runtime, selected by {@code dc3.mq.type} (default {@code rabbitmq}).
 *
 * @author pnoker
 * @since 2026.8.19
 */
public interface BrokerAdapter {

    /**
     * @return adapter type identifier, e.g. {@code "rabbitmq"}
     */
    String type();

    /**
     * @return what this broker can express natively
     */
    BrokerCapabilities capabilities();

    /**
     * Publish a serialized message.
     *
     * @param message the wire message
     */
    void publish(WireMqMessage message);

    /**
     * Publish with per-message confirmation.
     *
     * @param message      the wire message
     * @param confirmation invoked once the broker accepts/routes or fails the publish
     */
    void publish(WireMqMessage message, WireConfirmation confirmation);

    /**
     * Register a single-delivery subscription; the adapter declares any physical
     * destinations it needs (queues, bindings, consumer groups).
     *
     * @param spec     subscription declaration
     * @param listener raw delivery callback
     */
    void subscribe(SubscriptionSpec spec, RawDeliveryListener listener);

    /**
     * Register a batch subscription.
     *
     * @param spec     subscription declaration with {@code delivery=BATCH}
     * @param listener raw batch callback
     */
    void subscribeBatch(SubscriptionSpec spec, RawBatchListener listener);
}
