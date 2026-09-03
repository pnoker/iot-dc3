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

import io.github.pnoker.common.entity.bo.PointValueBO;
import java.util.List;
import reactor.core.publisher.Mono;

/** Reactive write orchestration for telemetry ingest. */
public interface PointValueIngestService {

    /** Save the value, inserting or updating as needed. */
    Mono<Boolean> saveValue(PointValueBO valueBO);

    /** Save the values, inserting or updating as needed. */
    Mono<List<PointValueBO>> saveValues(List<PointValueBO> valueBOList);

    /** Mark one receipt processed, returning the rows updated. */
    Mono<Void> markProcessed(List<PointValueBO> valueBOList);

    /** Replay leased/pending receipts after a process crash. */
    Mono<Integer> replayPending();
}
