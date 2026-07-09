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
 * Enumeration of notification channel type flags.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum NotifyChannelTypeEnum {

    /**
     * Feishu bot.
     */
    FEISHU_BOT((byte) 0, "feishu-bot", "Feishu Bot"),

    /**
     * Webhook.
     */
    WEBHOOK((byte) 1, "webhook", "Webhook"),

    /**
     * Email.
     */
    EMAIL((byte) 2, "email", "Email"),
    ;

    /**
     * Index.
     */
    @EnumValue
    private final Byte index;

    /**
     * Code.
     */
    private final String code;

    /**
     * Remark.
     */
    private final String remark;

    /**
     * Get enum by index.
     *
     * @param index Index
     * @return {@link NotifyChannelTypeEnum}
     */
    public static NotifyChannelTypeEnum ofIndex(Byte index) {
        Optional<NotifyChannelTypeEnum> any = Arrays.stream(NotifyChannelTypeEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by code.
     *
     * @param code Code
     * @return {@link NotifyChannelTypeEnum}
     */
    public static NotifyChannelTypeEnum ofCode(String code) {
        Optional<NotifyChannelTypeEnum> any = Arrays.stream(NotifyChannelTypeEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by name.
     *
     * @param name Name
     * @return {@link NotifyChannelTypeEnum}
     */
    public static NotifyChannelTypeEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

}
