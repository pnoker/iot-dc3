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
package io.github.pnoker.common.agentic.entity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Safe, structured visualization contract emitted by platform tools and
 * rendered by the frontend through a fixed chart whitelist.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2022.1.0
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AgenticVisualizationSpec implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;

    private String type;

    private String title;

    private String description;

    private List<Map<String, Object>> dataset;

    private Encode encode;

    private Map<String, Object> scale;

    private Map<String, Object> meta;

    private List<Annotation> annotations;

    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Encode implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String x;

        private String y;

        private String color;

        private String size;

        private String shape;

        public static Encode xy(String x, String y) {
            Encode encode = new Encode();
            encode.setX(x);
            encode.setY(y);
            return encode;
        }

    }

    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Annotation implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String type;

        private Object value;

        private String label;

    }

}
