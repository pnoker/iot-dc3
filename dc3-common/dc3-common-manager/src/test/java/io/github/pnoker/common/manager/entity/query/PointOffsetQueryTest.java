package io.github.pnoker.common.manager.entity.query;

import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointOffsetQueryTest {

    @Test
    void appliesDefaultsForMissingPagingValues() {
        PointOffsetQuery query = new PointOffsetQuery(null, null, null, null, null, null, null, null, null,
                null, null, null, null);

        assertThat(query.offset()).isZero();
        assertThat(query.limit()).isEqualTo(50);
        assertThat(query.sort()).isEmpty();
    }

    @Test
    void rejectsOutOfRangeOffsetAndLimit() {
        assertThatThrownBy(() -> new PointOffsetQuery(-1L, 10, List.of(), null, null, null, null, null, null, null,
                null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PointOffsetQuery(0L, 201, List.of(), null, null, null, null, null, null, null,
                null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnknownSortField() {
        SortSpec sort = new SortSpec("tenantId", SortSpec.Direction.ASC);
        assertThatThrownBy(() -> new PointOffsetQuery(0L, 10, List.of(sort), null, null, null, null, null, null,
                null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
