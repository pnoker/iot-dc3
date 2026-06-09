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

package io.github.pnoker.common.data.biz.alarm;

import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Notification channel adapter registry.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Service
public class NotifyChannelAdapterRegistry {

    private final Map<NotifyChannelTypeEnum, NotifyChannelAdapter> adapters =
            new EnumMap<>(NotifyChannelTypeEnum.class);

    public NotifyChannelAdapterRegistry(List<NotifyChannelAdapter> adapters) {
        for (NotifyChannelAdapter adapter : adapters) {
            this.adapters.put(adapter.channelType(), adapter);
        }
    }

    /**
     * Find adapter by channel type.
     *
     * @param channelTypeFlag channel type
     * @return adapter
     */
    public Optional<NotifyChannelAdapter> find(NotifyChannelTypeEnum channelTypeFlag) {
        return Optional.ofNullable(adapters.get(channelTypeFlag));
    }

}
