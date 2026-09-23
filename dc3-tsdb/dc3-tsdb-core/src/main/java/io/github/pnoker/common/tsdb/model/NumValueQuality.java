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
package io.github.pnoker.common.tsdb.model;

/**
 * Device-scoped sample-quality split inside a time window: how many stored rows carry a
 * numeric projection ({@code num_value} non-NULL) versus non-numeric payloads. Drives the
 * device detail dashboard's data-quality indicator.
 *
 * @param total      total stored rows in the window
 * @param numeric    rows with a non-NULL numeric projection
 * @param nonNumeric rows without a numeric projection ({@code total - numeric})
 * @author pnoker
 * @since 2026.9.24
 */
public record NumValueQuality(long total, long numeric, long nonNumeric) {

    /** Canonical constructor: rejects negative counts. */
    public NumValueQuality {
        if (total < 0 || numeric < 0 || nonNumeric < 0) {
            throw new IllegalArgumentException("quality counts must be non-negative");
        }
    }
}
