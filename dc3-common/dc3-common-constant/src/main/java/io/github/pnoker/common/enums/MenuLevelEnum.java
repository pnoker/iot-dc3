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

package io.github.pnoker.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enumeration of menu level flags.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum MenuLevelEnum {

    /**
     * Root menu
     */
    ROOT((byte) 0, "root", "Root menu"),

    /**
     * First-level submenu
     */
    C1((byte) 1, "c1", "First-level submenu"),

    /**
     * Second-level submenu
     */
    C2((byte) 2, "c2", "Second-level submenu"),

    /**
     * Third-level submenu
     */
    C3((byte) 3, "c3", "Third-level submenu"),

    /**
     * Fourth-level submenu
     */
    C4((byte) 4, "c4", "Fourth-level submenu"),
    ;

    /**
     * Index
     */
    @EnumValue
    private final Byte index;

    /**
     * Code
     */
    private final String code;

    /**
     * Remark
     */
    private final String remark;

    /**
     * Get enum by index
     *
     * @param index Index
     * @return {@link MenuLevelEnum}
     */
    public static MenuLevelEnum ofIndex(Byte index) {
        Optional<MenuLevelEnum> any = Arrays.stream(MenuLevelEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by code
     *
     * @param code Code
     * @return {@link MenuLevelEnum}
     */
    public static MenuLevelEnum ofCode(String code) {
        Optional<MenuLevelEnum> any = Arrays.stream(MenuLevelEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by name
     *
     * @param name Enum name
     * @return {@link MenuLevelEnum}
     */
    public static MenuLevelEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
