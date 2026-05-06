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
package io.github.pnoker.common.agentic.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the agentic module.
 *
 * <p>Prefix: {@code dc3.agentic}
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "dc3.agentic")
public class AgenticProperties {

    /**
     * Whether to enable the agentic module.
     */
    private boolean enabled = true;

    /**
     * Maximum number of messages retained per conversation for chat memory.
     */
    private int memoryMaxMessages = 50;

    /**
     * Session time-to-live in hours. Sessions older than this are marked expired.
     */
    private int sessionTtlHours = 72;
}
