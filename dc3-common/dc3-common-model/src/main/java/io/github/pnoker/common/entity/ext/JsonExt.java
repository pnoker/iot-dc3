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

package io.github.pnoker.common.entity.ext;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * JSON extension object for generic JSON-typed extension data.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "JSON extension object for generic JSON-typed extension data")
public class JsonExt implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Type, Json
     */
    @Schema(description = "Type of the JSON extension data")
    private String type;

    /**
     * , Json
     */
    @Schema(description = "JSON content payload of the extension data")
    private String content;

    /**
     * ,
     */
    @Builder.Default
    @Schema(description = "Version of the JSON extension data", example = "1")
    private Integer version = 1;

    /**
     * Description
     */
    @Schema(description = "Remark or description of the JSON extension data")
    private String remark;

}
