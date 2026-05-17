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
package io.github.pnoker.common.agentic.utils;

import io.github.pnoker.common.agentic.entity.model.AgenticVisualizationSpec;
import io.github.pnoker.common.constant.service.AgenticConstant;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticVisualizationUtilTest {

    @Test
    void numericSeriesFromNewestFirstBuildsChronologicalNumericRowsAndSummary() {
        AgenticVisualizationUtil.NumericSeries series = AgenticVisualizationUtil.numericSeriesFromNewestFirst(
                List.of("25.0", "true", "offline", "20.0"));

        assertThat(series.dataset()).hasSize(3);
        assertThat(series.dataset().get(0))
                .containsEntry(AgenticVisualizationUtil.FIELD_INDEX, 0)
                .containsEntry(AgenticVisualizationUtil.FIELD_VALUE, 20.0D);
        assertThat(series.dataset().get(1))
                .containsEntry(AgenticVisualizationUtil.FIELD_INDEX, 1)
                .containsEntry(AgenticVisualizationUtil.FIELD_VALUE, 1.0D);
        assertThat(series.dataset().get(2))
                .containsEntry(AgenticVisualizationUtil.FIELD_INDEX, 2)
                .containsEntry(AgenticVisualizationUtil.FIELD_VALUE, 25.0D);
        assertThat(series.summary().totalCount()).isEqualTo(4);
        assertThat(series.summary().numericCount()).isEqualTo(3);
        assertThat(series.summary().nonNumericCount()).isEqualTo(1);
        assertThat(series.summary().oldest()).isEqualTo(20.0D);
        assertThat(series.summary().latest()).isEqualTo(25.0D);
        assertThat(series.summary().delta()).isEqualTo(5.0D);
    }

    @Test
    void statBuildsWhitelistedStatVisualization() {
        AgenticVisualizationSpec spec = AgenticVisualizationUtil.stat("s1", "Summary", "Window summary",
                Map.of("latest", 23.5D), Map.of("pointId", 20L));

        assertThat(spec.getType()).isEqualTo(AgenticConstant.Visualization.Type.STAT);
        assertThat(spec.getDataset()).hasSize(1);
        assertThat(spec.getDataset().get(0)).containsEntry("latest", 23.5D);
        assertThat(spec.getMeta()).containsEntry("pointId", 20L);
    }

}
