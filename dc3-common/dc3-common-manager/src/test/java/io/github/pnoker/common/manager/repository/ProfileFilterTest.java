/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 */
package io.github.pnoker.common.manager.repository;

import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileFilterTest {
    @Test
    void rejectsInvalidTenantAndBounds() {
        assertThatThrownBy(() -> new ProfileFilter(0L, null, null, null, null, null,
                null, null, null, null, 0, 10, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProfileFilter(1L, null, null, null, null, null,
                null, null, null, null, -1, 10, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProfileFilter(1L, null, null, null, null, null,
                null, null, null, null, 0, 201, List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUntrustedSortField() {
        SortSpec invalid = new SortSpec("profile_name; drop table dc3_profile", SortSpec.Direction.ASC);
        assertThatThrownBy(() -> new ProfileFilter(1L, null, null, null, null, null,
                null, null, null, null, 0, 10, List.of(invalid))).isInstanceOf(IllegalArgumentException.class);
    }
}
