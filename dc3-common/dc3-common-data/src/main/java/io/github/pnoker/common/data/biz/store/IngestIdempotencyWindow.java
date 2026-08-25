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

package io.github.pnoker.common.data.biz.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Ingest-layer dedup window over recent {@code messageId}s — the port's
 * replacement for the retired {@code uk_point_value_event} unique index. It
 * absorbs MQ redeliveries inside the configured horizon; anything older that
 * slips through lands on the store's natural (series, deviceTime) upsert and
 * the fenced latest projection, both of which are idempotent. Entries are
 * marked only after the whole batch has been persisted, so a crash between
 * append and projection re-runs both paths cleanly.
 *
 * @author pnoker
 * @since 2026.8.20
 */
@Component
public class IngestIdempotencyWindow {

    private final Cache<String, Boolean> seen;

    public IngestIdempotencyWindow(
            @Value("${dc3.data.ingest.idempotency-size:100000}") long maximumSize,
            @Value("${dc3.data.ingest.idempotency-ttl-minutes:10}") long ttlMinutes) {
        this.seen = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(Duration.ofMinutes(ttlMinutes))
                .build();
    }

    /**
     * Return whether the message was already persisted inside the window.
     */
    public boolean seen(String messageId) {
        return seen.getIfPresent(messageId) != null;
    }

    /**
     * Record a message as persisted.
     */
    public void mark(String messageId) {
        seen.put(messageId, Boolean.TRUE);
    }
}
