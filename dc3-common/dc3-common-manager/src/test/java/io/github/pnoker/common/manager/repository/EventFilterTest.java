package io.github.pnoker.common.manager.repository;

import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventFilterTest {
    @Test void rejectsInvalidTenantAndBounds() {
        assertThatThrownBy(() -> new EventFilter(0L, null, null, null, null, null, null, null, null, 0, 10, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EventFilter(1L, null, null, null, null, null, null, null, null, -1, 10, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EventFilter(1L, null, null, null, null, null, null, null, null, 0, 201, List.of())).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsUntrustedSortField() {
        assertThatThrownBy(() -> new EventFilter(1L, null, null, null, null, null, null, null, null, 0, 10, List.of(new SortSpec("event_name;drop table", SortSpec.Direction.ASC)))).isInstanceOf(IllegalArgumentException.class);
    }
}
