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

package io.github.pnoker.common.init;

import io.github.pnoker.common.thread.entity.property.ThreadProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * Thread Initialization Runner for DC3 IoT Platform. This component handles thread
 * initialization tasks during application startup. It scans and initializes
 * thread-related components in the specified package.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@AutoConfiguration
@ComponentScan(basePackages = {"io.github.pnoker.common.thread"})
@EnableConfigurationProperties({ThreadProperties.class})
public class ThreadInitRunner implements ApplicationRunner {

    /**
     * Initialize thread components during application startup
     *
     * @param args Application arguments passed to the application
     * @throws Exception if initialization fails
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // nothing to do
    }

}
