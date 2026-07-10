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
 * Unified entity status enumeration for drivers and devices.
 * <p>
 * Persisted in {@code dc3_entity_state.entity_state_flag}. The {@code code} string is
 * used on MQ payloads and API responses.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum EntityStatusEnum {

    ONLINE((byte) 0, "online", "Online"),
    OFFLINE((byte) 1, "offline", "Offline"),
    MAINTAIN((byte) 2, "maintain", "Maintain"),
    FAULT((byte) 3, "fault", "Fault"),
    ;

    /**
     * Index value stored in database.
     */
    @EnumValue
    private final Byte index;

    /**
     * Status code string used on MQ payloads and APIs.
     */
    private final String code;

    /**
     * Human-readable description.
     */
    private final String remark;

    public static EntityStatusEnum ofIndex(Byte index) {
        Optional<EntityStatusEnum> any = Arrays.stream(EntityStatusEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    public static EntityStatusEnum ofCode(String code) {
        Optional<EntityStatusEnum> any = Arrays.stream(EntityStatusEnum.values())
                .filter(type -> type.getCode().equalsIgnoreCase(code))
                .findFirst();
        return any.orElse(null);
    }

    public static EntityStatusEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

}
