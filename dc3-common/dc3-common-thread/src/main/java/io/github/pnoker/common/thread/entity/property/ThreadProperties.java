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

package io.github.pnoker.common.thread.entity.property;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Thread Pool Configuration Properties
 * <p>
 * Configuration properties for thread pools in Spring Boot applications. Supports
 * configuration through application.yml with prefix "server.thread". Includes settings
 * for core pool size, maximum pool size, keep alive time, and thread name prefix.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "server.thread")
public class ThreadProperties {

    /**
     * Thread name prefix
     */
    @NotBlank(message = "Thread name prefix can't be empty")
    private String prefix = "dc3-thread-";

    /**
     * Number of thread pool core threads
     */
    @Min(value = 1, message = "Core pool size must be greater than 0")
    private int corePoolSize = 4;

    /**
     * Maximum number of thread pool threads
     */
    @Min(value = 1, message = "Maximum pool size must be greater than 0")
    private int maximumPoolSize = 32;

    /**
     * Idle thread waiting time, unit: seconds
     */
    @Min(value = 1, message = "Keep alive time must be greater than 0")
    private int keepAliveTime = 15;

    @AssertTrue(message = "Maximum pool size must be greater than or equal to core pool size")
    public boolean isPoolSizeValid() {
        return maximumPoolSize >= corePoolSize;
    }

}
