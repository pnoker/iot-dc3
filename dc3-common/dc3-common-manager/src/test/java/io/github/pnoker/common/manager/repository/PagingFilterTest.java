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

import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import java.util.List;
import org.junit.jupiter.api.Test;

class PagingFilterTest {

    @Test
    void bindingFilterPreservesCanonicalOffsetContract() {
        BindingFilter filter = new BindingFilter(
                9L,
                EntityTypeEnum.DEVICE,
                20L,
                30L,
                40,
                20,
                List.of(new SortSpec("createTime", SortSpec.Direction.DESC)));

        assertThat(filter.offset()).isEqualTo(40);
        assertThat(filter.limit()).isEqualTo(20);
        assertThat(filter.sort()).containsExactly(new SortSpec("createTime", SortSpec.Direction.DESC));
    }

    @Test
    void filtersRejectMissingTenantScope() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BindingFilter(0L, null, null, null, 0, 20, List.of()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LabelFilter(null, null, null, null, null, 0, 20, List.of()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new GroupFilter(-1L, null, null, null, null, null, 0, 20, List.of()));
    }

    @Test
    void filtersRejectUnindexedSortFields() {
        SortSpec unsupported = new SortSpec("tenantId", SortSpec.Direction.ASC);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BindingFilter(1L, null, null, null, 0, 20, List.of(unsupported)));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LabelFilter(1L, null, null, null, null, 0, 20, List.of(unsupported)));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new GroupFilter(1L, null, null, null, null, null, 0, 20, List.of(unsupported)));
    }
}
