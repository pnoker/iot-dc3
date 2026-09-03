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
package io.github.pnoker.common.entity.option;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/** Read-only option projection used by selection and tree controls. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Read-only dictionary option")
public record DictionaryOption(
        @Schema(description = "Logical option group", example = "device_type")
        String type,

        @Schema(
                description = "Human-readable display label",
                example = "Modbus TCP",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String label,

        @Schema(
                description = "Stable machine-readable value",
                example = "MODBUS_TCP",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String value,

        @Schema(
                description = "Whether the option is unavailable",
                example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean disabled,

        @Schema(
                description = "Whether the tree node is expanded by default",
                example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean expand,

        @Schema(description = "Child options; empty for a leaf", requiredMode = Schema.RequiredMode.REQUIRED)
        List<DictionaryOption> children) {

    public DictionaryOption {
        label = Objects.requireNonNull(label, "label must not be null");
        value = Objects.requireNonNull(value, "value must not be null");
        children = children == null ? List.of() : List.copyOf(children);
    }

    /** Create a leaf dictionary option. */
    public static DictionaryOption leaf(String label, String value) {
        return new DictionaryOption(null, label, value, false, false, List.of());
    }
}
