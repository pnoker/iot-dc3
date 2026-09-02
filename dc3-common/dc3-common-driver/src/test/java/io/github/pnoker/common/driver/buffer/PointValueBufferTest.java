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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies the SQLite DAO against a temporary on-disk database.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
class PointValueBufferTest {

    private static long epoch() {
        return System.currentTimeMillis() / 1000;
    }

    @Test
    void upsertSelectDeleteRoundtrip(@TempDir Path tmp) {
        PointValueBuffer buffer = newBuffer(tmp);
        long now = epoch();
        buffer.upsert(rec("id-1", 10L, 20L, 1, now, now));
        buffer.upsert(rec("id-2", 11L, 21L, 1, now, now));

        assertThat(buffer.selectPending(10, now)).hasSize(2);
        buffer.delete("id-1");
        assertThat(buffer.selectPending(10, now)).hasSize(1);
        buffer.close();
    }

    @Test
    void selectPendingSkipsFutureAttempts(@TempDir Path tmp) {
        PointValueBuffer buffer = newBuffer(tmp);
        long now = epoch();
        buffer.upsert(rec("due", 10L, 20L, 1, now, now));
        buffer.upsert(rec("future", 11L, 21L, 1, now + 3600, now));

        List<BufferedPointValue> pending = buffer.selectPending(10, now);
        assertThat(pending).hasSize(1).extracting(BufferedPointValue::id).contains("due");
        buffer.close();
    }

    @Test
    void markRetryBumpsAttemptAndBackoff(@TempDir Path tmp) {
        PointValueBuffer buffer = newBuffer(tmp);
        long now = epoch();
        buffer.upsert(rec("id", 10L, 20L, 1, now, now));

        buffer.markRetry("id", 2, now + 60);
        assertThat(buffer.selectPending(10, now)).isEmpty();
        List<BufferedPointValue> later = buffer.selectPending(10, now + 60);
        assertThat(later).hasSize(1);
        assertThat(later.get(0).attempt()).isEqualTo(2);
        buffer.close();
    }

    @Test
    void upsertReplacesExistingRow(@TempDir Path tmp) {
        PointValueBuffer buffer = newBuffer(tmp);
        long now = epoch();
        buffer.upsert(rec("id", 10L, 20L, 1, now, now));
        buffer.upsert(rec("id", 10L, 20L, 2, now + 30, now));

        assertThat(buffer.count()).isEqualTo(1);
        assertThat(buffer.selectPending(10, now + 30).get(0).attempt()).isEqualTo(2);
        buffer.close();
    }

    @Test
    void upsertBatchCommitsEveryRow(@TempDir Path tmp) {
        PointValueBuffer buffer = newBuffer(tmp);
        long now = epoch();

        buffer.upsertBatch(List.of(rec("batch-1", 10L, 20L, 0, now, now), rec("batch-2", 11L, 21L, 0, now, now)));

        assertThat(buffer.selectPending(10, now))
                .extracting(BufferedPointValue::id)
                .containsExactly("batch-1", "batch-2");
        buffer.close();
    }

    @Test
    void committedRowsSurviveReopen(@TempDir Path tmp) {
        Path db = tmp.resolve("buffer.db");
        long now = epoch();
        PointValueBuffer first = new PointValueBuffer(db.toString());
        first.initialize();
        first.upsert(rec("durable", 10L, 20L, 0, now, now));
        first.close();

        PointValueBuffer reopened = new PointValueBuffer(db.toString());
        reopened.initialize();
        assertThat(reopened.selectPending(10, now))
                .singleElement()
                .extracting(BufferedPointValue::id)
                .isEqualTo("durable");
        reopened.close();
    }

    private PointValueBuffer newBuffer(Path tmp) {
        PointValueBuffer buffer = new PointValueBuffer(tmp.resolve("buffer.db").toString());
        buffer.initialize();
        return buffer;
    }

    private BufferedPointValue rec(
            String id, Long deviceId, Long pointId, int attempt, long nextAttemptAt, long createdAt) {
        return new BufferedPointValue(id, deviceId, pointId, 1L, 2L, "{}", "rk", attempt, nextAttemptAt, createdAt);
    }
}
