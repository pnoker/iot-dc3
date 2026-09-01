package io.github.pnoker.common.manager.grpc;

import io.github.pnoker.api.common.PageRequest;
import io.github.pnoker.api.common.SortDirection;
import io.github.pnoker.api.common.SortSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GrpcPageUtilTest {

    @Test
    void rejectsMissingAndInvalidPage() {
        assertThrows(IllegalArgumentException.class, () -> GrpcPageUtil.require(null));
        assertThrows(IllegalArgumentException.class, () -> GrpcPageUtil.require(
                PageRequest.newBuilder().setOffset(-1).setLimit(10).build()));
        assertThrows(IllegalArgumentException.class, () -> GrpcPageUtil.require(
                PageRequest.newBuilder().setOffset(0).setLimit(0).build()));
    }

    @Test
    void convertsSortWithoutRepairingUnspecifiedDirection() {
        PageRequest page = PageRequest.newBuilder().setOffset(4).setLimit(8)
                .addSort(SortSpec.newBuilder().setField("name").setDirection(SortDirection.SORT_DIRECTION_DESC))
                .build();

        var converted = GrpcPageUtil.require(page);

        assertEquals(4, converted.offset());
        assertEquals(8, converted.limit());
        assertEquals("name", converted.sort().getFirst().field());
        assertEquals(io.github.pnoker.db.r2dbc.core.page.SortSpec.Direction.DESC,
                converted.sort().getFirst().direction());
        assertThrows(IllegalArgumentException.class, () -> GrpcPageUtil.require(
                PageRequest.newBuilder().setOffset(0).setLimit(8)
                        .addSort(SortSpec.newBuilder().setField("name")).build()));
    }
}
