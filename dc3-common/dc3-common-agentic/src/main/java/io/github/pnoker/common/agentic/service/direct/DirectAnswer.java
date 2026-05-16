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

import java.util.List;

/**
 * Structured direct response for deterministic backend answers.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
public record DirectAnswer(String title, String message, List<Field> fields, List<Table> tables, List<Chart> charts) {

    public DirectAnswer {
        fields = List.copyOf(fields == null ? List.of() : fields);
        tables = List.copyOf(tables == null ? List.of() : tables);
        charts = List.copyOf(charts == null ? List.of() : charts);
    }

    public static DirectAnswer message(String title, String message) {
        return new DirectAnswer(title, message, List.of(), List.of(), List.of());
    }

    public static DirectAnswer table(String title, String message, List<Field> fields, List<Table> tables,
                                     List<Chart> charts) {
        return new DirectAnswer(title, message, fields, tables, charts);
    }

    public record Field(String name, String value) {
    }

    public record Table(String title, List<String> headers, List<List<String>> rows) {

        public Table {
            headers = List.copyOf(headers == null ? List.of() : headers);
            rows = List.copyOf(rows == null ? List.of() : rows);
        }

    }

    public record Chart(String type, String title, String unit, String xLabel, String xType,
                        List<Series> series) {

        public Chart {
            series = List.copyOf(series == null ? List.of() : series);
        }

    }

    public record Series(String name, List<List<Number>> data) {

        public Series {
            data = List.copyOf(data == null ? List.of() : data);
        }

    }

}
