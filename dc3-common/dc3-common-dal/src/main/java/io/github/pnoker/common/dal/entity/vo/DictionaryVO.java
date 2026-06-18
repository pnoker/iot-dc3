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

package io.github.pnoker.common.dal.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Dictionary view object (VO) representing hierarchical dictionary items.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "Dictionary view object")
public class DictionaryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Dictionary type.
     */
    @Schema(description = "Logical grouping code of the dictionary item, e.g. device_type, point_rw.", example = "device_type")
    private String type;

    /**
     * Dictionary label name.
     */
    @Schema(description = "Human-readable display label for the dictionary item.", example = "Modbus TCP")
    private String label;

    /**
     * Dictionary label value.
     */
    @Schema(description = "Stable machine-readable value paired with the label.", example = "MODBUS_TCP")
    private String value;

    /**
     * Whether the dictionary item is disabled.
     */
    @Schema(description = "Whether the dictionary item is hidden or unavailable for selection.", example = "false")
    private boolean disabled;

    /**
     * Whether the dictionary node is expanded.
     */
    @Schema(description = "Whether the dictionary node is expanded by default in the tree view.", example = "true")
    private boolean expand;

    /**
     * Child dictionary nodes.
     */
    @Schema(description = "Child dictionary nodes forming the hierarchical tree; empty when this is a leaf item.")
    private List<DictionaryVO> children;

}
