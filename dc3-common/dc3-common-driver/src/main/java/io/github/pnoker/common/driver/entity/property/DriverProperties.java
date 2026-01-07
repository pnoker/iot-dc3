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
 * driver
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "driver")
public class DriverProperties {

    /**
     * Tenant
     */
    @NotBlank(message = "租户不能为空")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$", message = "无效的租户")
    private String tenant;

    /**
     * Type
     */
    @NotNull(message = "驱动类型不能为空")
    private DriverTypeFlagEnum type = DriverTypeFlagEnum.DRIVER_CLIENT;

    /**
     * Name
     */
    @NotBlank(message = "驱动名称不能为空")
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$", message = "驱动名称格式无效")
    private String name;

    /**
     * Driver ID
     */
    @NotBlank(message = "驱动编号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$", message = "无效的驱动编号")
    private String code;

    /**
     * Description
     */
    private String remark;

    /**
     *
     */
    private ScheduleProperties schedule;

    /**
     *
     */
    private List<DriverAttributeDTO> driverAttribute;

    /**
     *
     */
    private List<PointAttributeDTO> pointAttribute;

    /**/
    /**
     * Code, 8
     */
    private String node;

    /**
     * Driver service name, Tenant/Name
     */
    private String service;

    /**
     *
     */
    private String host;

    /**
     *
     */
    private Integer port;

    /**
     * , Tenant/Name_Code
     */
    private String client;

    /**
     * driver.schedule
     *
     * @author pnoker
     * @version 2025.9.0
     * @since 2022.1.0
     */
    @Getter
    @Setter
    public static class ScheduleProperties {

        /**
         *
         */
        private ScheduleConfig read;

        /**
         *
         */
        private ScheduleConfig custom;

        /**
         *
         *
         * @author pnoker
         * @version 2025.9.0
         * @since 2022.1.0
         */
        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ScheduleConfig {
            private Boolean enable = false;
            private String cron = "* */15 * * * ?";
        }
    }
}
