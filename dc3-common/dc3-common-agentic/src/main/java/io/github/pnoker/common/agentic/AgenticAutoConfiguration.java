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
package io.github.pnoker.common.agentic;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for the agentic module.
 * <p>
 * Activated when {@code dc3.agentic.enabled=true} (default). Scans all components under
 * {@code io.github.pnoker.common.agentic}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@AutoConfiguration
@ConditionalOnProperty(name = "dc3.agentic.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan("io.github.pnoker.common.agentic")
@MapperScan("io.github.pnoker.common.agentic.mapper")
public class AgenticAutoConfiguration {

}
