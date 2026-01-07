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
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Dictionary view object (VO) representing hierarchical dictionary items.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DictionaryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Dictionary type.
     */
    private String type;

    /**
     * Dictionary label name.
     */
    private String label;

    /**
     * Dictionary label value.
     */
    private String value;

    /**
     * Whether the dictionary item is disabled.
     */
    private boolean disabled;

    /**
     * Whether the dictionary node is expanded.
     */
    private boolean expand;

    /**
     * Child dictionary nodes.
     */
    private List<DictionaryVO> children;
}
