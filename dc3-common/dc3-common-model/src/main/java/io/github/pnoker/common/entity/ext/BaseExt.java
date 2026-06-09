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
 * Base JSON extension object with a typed version field.
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
@Schema(description = "Base JSON extension object with a typed version field, embedded inside VO extension fields")
public class BaseExt implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Type, used to parse JSON strings.
     */
    @Schema(description = "Type identifier used to parse the JSON extension string into the proper subtype")
    private String type;

    /**
     * Version, used for optimistic locking.
     */
    @Schema(description = "Version number used for optimistic locking", example = "1")
    @Builder.Default
    private Integer version = 1;

    /**
     * Description.
     */
    @Schema(description = "Description / remark of the extension object")
    private String remark;

}
