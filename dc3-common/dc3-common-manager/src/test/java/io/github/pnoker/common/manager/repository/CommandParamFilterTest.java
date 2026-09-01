package io.github.pnoker.common.manager.repository;

import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandParamFilterTest {
    @Test void rejectsMissingTenant() { assertThatThrownBy(() -> new CommandParamFilter(null, null, null, null, null, null, null, null, 0, 20, List.of())).isInstanceOf(IllegalArgumentException.class); }
    @Test void rejectsUnknownSortField() { assertThatThrownBy(() -> new CommandParamFilter(7L, null, null, null, null, null, null, null, 0, 20, List.of(new SortSpec("tenantId", SortSpec.Direction.ASC)))).isInstanceOf(IllegalArgumentException.class); }
}
