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
import io.github.pnoker.common.constant.common.BaseConstant;
import io.github.pnoker.common.constant.service.AgenticConstant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builders for safe agentic visualization specs.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
public class AgenticVisualizationUtil {

    public static final String FIELD_INDEX = "index";

    public static final String FIELD_VALUE = "value";

    public static final String FIELD_SERIES = "series";

    private AgenticVisualizationUtil() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

    public static NumericSeries numericSeriesFromNewestFirst(List<String> values) {
        if (Objects.isNull(values) || values.isEmpty()) {
            return new NumericSeries(List.of(), NumericSummary.empty(0));
        }

        List<Map<String, Object>> dataset = new ArrayList<>();
        List<Double> numericValues = new ArrayList<>();
        int rendered = 0;
        for (int i = values.size() - 1; i >= 0; i--) {
            Double value = parseNumericValue(values.get(i));
            if (Objects.isNull(value)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put(FIELD_INDEX, rendered);
            row.put(FIELD_VALUE, value);
            row.put(FIELD_SERIES, FIELD_VALUE);
            dataset.add(row);
            numericValues.add(value);
            rendered++;
        }
        return new NumericSeries(List.copyOf(dataset), summarize(values.size(), numericValues));
    }

    public static AgenticVisualizationSpec line(String id, String title, String description,
                                                List<Map<String, Object>> dataset,
                                                AgenticVisualizationSpec.Encode encode,
                                                Map<String, Object> meta,
                                                List<AgenticVisualizationSpec.Annotation> annotations) {
        AgenticVisualizationSpec spec = new AgenticVisualizationSpec();
        spec.setId(id);
        spec.setType(AgenticConstant.Visualization.Type.LINE);
        spec.setTitle(title);
        spec.setDescription(description);
        spec.setDataset(List.copyOf(Objects.requireNonNullElse(dataset, List.of())));
        spec.setEncode(Objects.requireNonNullElseGet(encode,
                () -> AgenticVisualizationSpec.Encode.xy(FIELD_INDEX, FIELD_VALUE)));
        spec.setScale(Map.of("x", AgenticConstant.Visualization.Scale.LINEAR,
                "y", AgenticConstant.Visualization.Scale.LINEAR));
        spec.setMeta(Map.copyOf(Objects.requireNonNullElse(meta, Map.of())));
        spec.setAnnotations(List.copyOf(Objects.requireNonNullElse(annotations, List.of())));
        return spec;
    }

    public static AgenticVisualizationSpec stat(String id, String title, String description,
                                                Map<String, Object> row, Map<String, Object> meta) {
        AgenticVisualizationSpec spec = new AgenticVisualizationSpec();
        spec.setId(id);
        spec.setType(AgenticConstant.Visualization.Type.STAT);
        spec.setTitle(title);
        spec.setDescription(description);
        spec.setDataset(Objects.isNull(row) || row.isEmpty() ? List.of() : List.of(new LinkedHashMap<>(row)));
        spec.setEncode(new AgenticVisualizationSpec.Encode());
        spec.setMeta(Map.copyOf(Objects.requireNonNullElse(meta, Map.of())));
        return spec;
    }

    public static AgenticVisualizationSpec.Annotation yAnnotation(Double value, String label) {
        AgenticVisualizationSpec.Annotation annotation = new AgenticVisualizationSpec.Annotation();
        annotation.setType("y");
        annotation.setValue(value);
        annotation.setLabel(label);
        return annotation;
    }

    public static Map<String, Object> pointHistoryMeta(Long deviceId, Long pointId, String valueSource) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("deviceId", deviceId);
        meta.put("pointId", pointId);
        meta.put("valueSource", valueSource);
        return meta;
    }

    public static Map<String, Object> statRow(NumericSummary summary) {
        if (Objects.isNull(summary) || summary.numericCount() < 1) {
            return Map.of();
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("latest", summary.latest());
        row.put("average", summary.average());
        row.put("min", summary.min());
        row.put("max", summary.max());
        row.put("delta", summary.delta());
        row.put("numericCount", summary.numericCount());
        row.put("nonNumericCount", summary.nonNumericCount());
        return row;
    }

    private static Double parseNumericValue(String raw) {
        if (Objects.isNull(raw)) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        if ("true".equalsIgnoreCase(value)) {
            return 1D;
        }
        if ("false".equalsIgnoreCase(value)) {
            return 0D;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (Double.isFinite(parsed)) {
                return parsed;
            }
            return null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static NumericSummary summarize(int totalCount, List<Double> values) {
        if (values.isEmpty()) {
            return NumericSummary.empty(totalCount);
        }
        double min = values.getFirst();
        double max = values.getFirst();
        double sum = 0D;
        for (Double value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
        }
        double oldest = values.getFirst();
        double latest = values.getLast();
        return new NumericSummary(totalCount, values.size(), totalCount - values.size(), oldest, latest,
                latest - oldest, min, max, sum / values.size());
    }

    public record NumericSeries(List<Map<String, Object>> dataset, NumericSummary summary) {

        public NumericSeries {
            dataset = List.copyOf(Objects.requireNonNullElse(dataset, List.of()));
            summary = Objects.requireNonNullElseGet(summary, () -> NumericSummary.empty(0));
        }

    }

    public record NumericSummary(int totalCount, int numericCount, int nonNumericCount, Double oldest, Double latest,
                                 Double delta, Double min, Double max, Double average) {

        public static NumericSummary empty(int totalCount) {
            return new NumericSummary(totalCount, 0, Math.max(totalCount, 0), null, null, null, null, null, null);
        }

    }

}
