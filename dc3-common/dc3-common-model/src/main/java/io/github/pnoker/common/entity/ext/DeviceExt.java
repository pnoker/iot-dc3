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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * JSON extension object for device metadata and configuration.
 *
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Schema(description = "JSON extension object for device metadata and configuration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceExt extends BaseExt {

    /**
     *
     * <p>
     * Type Version
     */
    @Schema(description = "Device extension content payload carrying type-specific configuration")
    private Content content;

    @Schema(description = "Device extension content payload carrying type-specific configuration")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "Reserved configuration field used to retain custom device data")
        private String keep;

    }

}
