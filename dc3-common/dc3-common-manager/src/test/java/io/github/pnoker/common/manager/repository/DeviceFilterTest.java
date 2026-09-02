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
package io.github.pnoker.common.manager.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeviceFilterTest {

    @Test
    void copiesSortAndDefaultsOptionalCriteria() {
        var sort = new java.util.ArrayList<>(List.of(new SortSpec("deviceName", SortSpec.Direction.ASC)));
        DeviceFilter filter = new DeviceFilter(7L, "sensor", null, 20L, null, EnableFlagEnum.ENABLE, 0, 50, sort);

        sort.clear();
        assertThat(filter.sort()).containsExactly(new SortSpec("deviceName", SortSpec.Direction.ASC));
        assertThat(filter.version()).isNull();
        assertThat(filter.groupId()).isNull();
        assertThat(filter.labelId()).isNull();
    }

    @Test
    void rejectsInvalidTenantOffsetAndLimit() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DeviceFilter(0L, null, null, null, null, null, 0, 50, List.of()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DeviceFilter(1L, null, null, null, null, null, -1, 50, List.of()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DeviceFilter(1L, null, null, null, null, null, 0, 201, List.of()));
    }
}
