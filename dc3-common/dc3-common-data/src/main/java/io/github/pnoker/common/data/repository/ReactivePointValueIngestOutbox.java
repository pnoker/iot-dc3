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
package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.model.PointValueDO;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Durable relational receipt for point-value ingestion. */
public interface ReactivePointValueIngestOutbox {

    /**
     * Insert new receipts and claim them for the supplied owner in one
     * transaction. Existing receipts are never re-claimed by a duplicate
     * delivery.
     */
    Mono<List<PointValueDO>> enqueue(List<PointValueDO> values, String owner);

    /** Emit receipts that have already reached the durable store. */
    Flux<PointValueDO> findPersisted(List<PointValueDO> values);

    /** Mark one receipt persisted for the owner, returning the rows updated. */
    Mono<Integer> markPersisted(PointValueDO value, String owner);

    /** Claim up to the limit of pending receipts for the replay owner. */
    Flux<PointValueDO> claim(String owner, int limit);

    /** Mark one receipt processed, returning the rows updated. */
    Mono<Integer> markProcessed(PointValueDO value);

    /** Mark one receipt failed for the owner with the error, returning the rows updated. */
    Mono<Integer> markFailed(PointValueDO value, String owner, String error);
}
