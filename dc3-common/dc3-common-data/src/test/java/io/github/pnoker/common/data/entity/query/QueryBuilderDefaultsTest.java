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

package io.github.pnoker.common.data.entity.query;

import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryBuilderDefaultsTest {

    @Test
    void buildersPreservePagingDefaults() {
        assertDefaults(MessageQuery.builder().build().getLimit(), MessageQuery.builder().build().getSort());
        assertDefaults(NotifyChannelBindQuery.builder().build().getLimit(),
                NotifyChannelBindQuery.builder().build().getSort());
        assertDefaults(NotifyChannelQuery.builder().build().getLimit(), NotifyChannelQuery.builder().build().getSort());
        assertDefaults(NotifyHistoryQuery.builder().build().getLimit(), NotifyHistoryQuery.builder().build().getSort());
        assertDefaults(NotifyQuery.builder().build().getLimit(), NotifyQuery.builder().build().getSort());
        assertDefaults(RuleQuery.builder().build().getLimit(), RuleQuery.builder().build().getSort());
        assertDefaults(RuleStateQuery.builder().build().getLimit(), RuleStateQuery.builder().build().getSort());
    }

    private void assertDefaults(int limit, java.util.List<?> sort) {
        assertThat(limit).isEqualTo(PageRequest.DEFAULT_LIMIT);
        assertThat(sort).isEmpty();
    }

}
