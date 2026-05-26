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
 * Melsec point variable holding a device address string and type code.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2016.10.1
 */
@Getter
public class MelsecPointVariable {

    private final String address;

    private final String type;

    private final int length;

    public MelsecPointVariable(String address, String type) {
        this(address, type, 0);
    }

    public MelsecPointVariable(String address, String type, int length) {
        this.address = address;
        this.type = type;
        this.length = length;
    }

}
