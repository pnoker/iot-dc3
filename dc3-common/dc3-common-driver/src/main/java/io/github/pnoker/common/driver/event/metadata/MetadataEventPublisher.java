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

package io.github.pnoker.common.driver.event.metadata;

import io.github.pnoker.common.entity.event.MetadataEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes internal Spring metadata events so driver-specific components can react to
 * metadata changes.
 *
 * @author zhangzi
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetadataEventPublisher {

    /** Spring publisher used to broadcast metadata events within the application context. */
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Publishes the supplied metadata event to the Spring application context.
     *
     * @param metadataEvent metadata event
     */
    public void publishEvent(MetadataEvent metadataEvent) {
        log.debug("Metadata event publisher publishEvent: id={}, type={}", metadataEvent.getId(), metadataEvent.getMetadataType());
        applicationEventPublisher.publishEvent(metadataEvent);
    }

}
