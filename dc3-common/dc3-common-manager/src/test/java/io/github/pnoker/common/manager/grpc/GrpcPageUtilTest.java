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
package io.github.pnoker.common.manager.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.pnoker.api.common.PageRequest;
import io.github.pnoker.api.common.SortDirection;
import io.github.pnoker.api.common.SortSpec;
import org.junit.jupiter.api.Test;

class GrpcPageUtilTest {

    @Test
    void rejectsMissingAndInvalidPage() {
        assertThrows(IllegalArgumentException.class, () -> GrpcPageUtil.require(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> GrpcPageUtil.require(
                        PageRequest.newBuilder().setOffset(-1).setLimit(10).build()));
        assertThrows(
                IllegalArgumentException.class,
                () -> GrpcPageUtil.require(
                        PageRequest.newBuilder().setOffset(0).setLimit(0).build()));
    }

    @Test
    void convertsSortWithoutRepairingUnspecifiedDirection() {
        PageRequest page = PageRequest.newBuilder()
                .setOffset(4)
                .setLimit(8)
                .addSort(SortSpec.newBuilder().setField("name").setDirection(SortDirection.SORT_DIRECTION_DESC))
                .build();

        var converted = GrpcPageUtil.require(page);

        assertEquals(4, converted.offset());
        assertEquals(8, converted.limit());
        assertEquals("name", converted.sort().getFirst().field());
        assertEquals(
                io.github.pnoker.db.r2dbc.core.page.SortSpec.Direction.DESC,
                converted.sort().getFirst().direction());
        assertThrows(
                IllegalArgumentException.class,
                () -> GrpcPageUtil.require(PageRequest.newBuilder()
                        .setOffset(0)
                        .setLimit(8)
                        .addSort(SortSpec.newBuilder().setField("name"))
                        .build()));
    }
}
