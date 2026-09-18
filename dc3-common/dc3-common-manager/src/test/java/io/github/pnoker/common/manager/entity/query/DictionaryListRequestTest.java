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
package io.github.pnoker.common.manager.entity.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class DictionaryListRequestTest {

    private final ObjectMapper objectMapper = JsonUtil.getJsonMapper();

    @Test
    void appliesDefaultsWhenPagingFieldsAreMissingFromJson() throws Exception {
        DictionaryListRequest request = objectMapper.readValue("{}", DictionaryListRequest.class);

        assertThat(request.offset()).isZero();
        assertThat(request.limit()).isEqualTo(50);
        assertThat(request.sort()).isEmpty();
    }

    @Test
    void rejectsUnsupportedSortField() {
        assertThatThrownBy(() -> new DictionaryListRequest(
                        0L, 50, List.of(new SortSpec("tenantId", SortSpec.Direction.ASC)), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported dictionary sort field");
    }
}
