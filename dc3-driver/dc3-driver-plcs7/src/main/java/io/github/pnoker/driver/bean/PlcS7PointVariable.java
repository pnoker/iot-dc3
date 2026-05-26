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

package io.github.pnoker.driver.bean;

import lombok.Getter;

/**
 * Simplified S7 point variable holding an S7 address string and type code.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2016.10.1
 */
@Getter
public class PlcS7PointVariable {

    private final String address;

    private final String type;

    public PlcS7PointVariable(int dbNum, int byteOffset, int bitOffset, String type) {
        this.type = type;
        if ("boolean".equals(type) || "bool".equals(type)) {
            this.address = String.format("DB%d.%d.%d", dbNum, byteOffset, bitOffset);
        } else {
            this.address = String.format("DB%d.%d", dbNum, byteOffset);
        }
    }

}
