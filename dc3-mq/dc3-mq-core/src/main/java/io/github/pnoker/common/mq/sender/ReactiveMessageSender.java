/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.pnoker.common.mq.sender;

import io.github.pnoker.common.mq.message.MqMessage;
import reactor.core.publisher.Mono;

/**
 * Non-blocking message publication contract for reactive business pipelines.
 */
public interface ReactiveMessageSender {

    /**
     * Publishes a message and completes only after the broker confirmation callback.
     * Cancellation is propagated by ignoring a late callback after disposal.
     *
     * @param message message to publish
     * @return completion signal
     */
    Mono<Void> sendConfirmed(MqMessage message);
}
