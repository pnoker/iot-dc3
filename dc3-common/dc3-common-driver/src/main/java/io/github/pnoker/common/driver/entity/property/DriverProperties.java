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

package io.github.pnoker.common.driver.entity.property;

import io.github.pnoker.common.driver.entity.dto.DriverAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeDTO;
import io.github.pnoker.common.enums.DriverTypeFlagEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Spring configuration properties for a driver instance.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "dc3.driver")
public class DriverProperties {

    /**
     * Tenant name of the driver instance.
     */
    @NotBlank(message = "Tenant can't be empty")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$", message = "Invalid tenant")
    private String tenant;

    /**
     * Driver runtime type.
     */
    @NotNull(message = "Driver type can't be empty")
    private DriverTypeFlagEnum type = DriverTypeFlagEnum.DRIVER_CLIENT;

    /**
     * Driver display name.
     */
    @NotBlank(message = "Driver name can't be empty")
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$", message = "Invalid driver name format")
    private String name;

    /**
     * Driver code defined in configuration.
     */
    @NotBlank(message = "Driver code can't be empty")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$", message = "Invalid driver code")
    private String code;

    /**
     * Description
     */
    private String remark;

    /**
     * Schedule configuration for periodic driver tasks.
     */
    @Valid
    private ScheduleProperties schedule;

    /**
     * Driver-level attribute definitions declared in configuration.
     */
    private List<@Valid DriverAttributeDTO> driverAttribute;

    /**
     * Point-level attribute definitions declared in configuration.
     */
    private List<@Valid PointAttributeDTO> pointAttribute;

    /**
     * Generated or configured driver node identifier.
     */
    private String node;

    /**
     * Driver service name, typically composed from tenant and application name.
     */
    private String service;

    /**
     * Host address exposed by the driver process.
     */
    private String host;

    /**
     * Service port exposed by the driver process.
     */
    private Integer port;

    /**
     * Driver client identifier used for queue and registration routing.
     */
    private String client;

    /**
     * Scheduling options for built-in driver jobs.
     */
    @Getter
    @Setter
    public static class ScheduleProperties {

        /**
         * Periodic read job configuration.
         */
        @Valid
        private ScheduleConfig read = new ScheduleConfig();

        /**
         * Custom job configuration.
         */
        @Valid
        private ScheduleConfig custom = new ScheduleConfig();

        /**
         * Cron-based schedule definition.
         */
        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ScheduleConfig {

            /**
             * Whether the job is enabled.
             */
            private Boolean enable = false;

            /**
             * Quartz cron expression used by the job.
             */
            private String cron = "* */15 * * * ?";

        }

    }

}
