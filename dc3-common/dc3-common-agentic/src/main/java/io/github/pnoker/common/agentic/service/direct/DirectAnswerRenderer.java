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
package io.github.pnoker.common.agentic.service.direct;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Renders structured direct answers into the chat text surface.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DirectAnswerRenderer {

    private final ObjectMapper objectMapper;

    public DirectAnswerRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String render(DirectAnswer answer) {
        if (Objects.isNull(answer)) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotBlank(answer.title())) {
            builder.append("### ").append(answer.title().trim()).append("\n\n");
        }
        if (StringUtils.isNotBlank(answer.message())) {
            builder.append(answer.message().trim()).append("\n\n");
        }
        if (!answer.fields().isEmpty()) {
            builder.append("| 字段 | 值 |\n");
            builder.append("| --- | --- |\n");
            for (DirectAnswer.Field field : answer.fields()) {
                builder.append("| ").append(tableText(field.name())).append(" | ")
                        .append(tableText(field.value())).append(" |\n");
            }
            builder.append('\n');
        }
        for (DirectAnswer.Table table : answer.tables()) {
            appendTable(builder, table);
        }
        for (DirectAnswer.Chart chart : answer.charts()) {
            appendChart(builder, chart);
        }
        return builder.toString().trim();
    }

    private void appendTable(StringBuilder builder, DirectAnswer.Table table) {
        if (Objects.isNull(table) || table.headers().isEmpty()) {
            return;
        }
        if (StringUtils.isNotBlank(table.title())) {
            builder.append(table.title().trim()).append(":\n");
        }
        builder.append("| ");
        builder.append(String.join(" | ", table.headers().stream().map(this::tableText).toList()));
        builder.append(" |\n| ");
        builder.append(String.join(" | ", table.headers().stream().map(ignored -> "---").toList()));
        builder.append(" |\n");
        for (List<String> row : table.rows()) {
            builder.append("| ");
            builder.append(String.join(" | ", row.stream().map(this::tableText).toList()));
            builder.append(" |\n");
        }
        builder.append('\n');
    }

    private void appendChart(StringBuilder builder, DirectAnswer.Chart chart) {
        if (Objects.isNull(chart) || chart.series().isEmpty()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", chart.title());
        payload.put("unit", chart.unit());
        payload.put("xLabel", chart.xLabel());
        payload.put("xType", chart.xType());
        payload.put("series", chart.series());
        builder.append("```chart:").append(StringUtils.defaultIfBlank(chart.type(), "line")).append('\n');
        builder.append(toJson(payload)).append('\n');
        builder.append("```\n\n");
    }

    private String tableText(String value) {
        return StringUtils.defaultString(value).replace("|", "\\|").replace("\n", " ");
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (DatabindException e) {
            log.warn("Direct answer chart serialization failed", e);
            return "{}";
        }
    }

}
