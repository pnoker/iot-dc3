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
 * 驱动配置文件 driver 字段内容
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "driver")
public class DriverProperties {

    /**
     * 租户
     */
    @NotBlank(message = "租户不能为空")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$", message = "无效的租户")
    private String tenant;

    /**
     * 驱动类型
     */
    @NotNull(message = "驱动类型不能为空")
    private DriverTypeFlagEnum type = DriverTypeFlagEnum.DRIVER_CLIENT;

    /**
     * 驱动名称
     */
    @NotBlank(message = "驱动名称不能为空")
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$", message = "驱动名称格式无效")
    private String name;

    /**
     * 驱动编号
     */
    @NotBlank(message = "驱动编号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$", message = "无效的驱动编号")
    private String code;

    /**
     * 描述
     */
    private String remark;

    /**
     * 定时任务相关属性
     */
    private ScheduleProperties schedule;

    /**
     * 驱动属性
     */
    private List<DriverAttributeDTO> driverAttribute;

    /**
     * 位号属性
     */
    private List<PointAttributeDTO> pointAttribute;

    /*以下定义为内部参数*/
    /**
     * 驱动节点编号, 8位随机数
     */
    private String node;

    /**
     * 驱动服务名称, 租户/应用名称
     */
    private String service;

    /**
     * 驱动主机
     */
    private String host;

    /**
     * 驱动端口
     */
    private Integer port;

    /**
     * 驱动客户端, 租户/应用名称_驱动节点编号
     */
    private String client;

    /**
     * 驱动配置文件 driver.schedule 字段内容
     *
     * @author pnoker
     * @version 2025.6.0
     * @since 2022.1.0
     */
    @Getter
    @Setter
    public static class ScheduleProperties {

        /**
         * 读任务配置
         */
        private ScheduleConfig read;

        /**
         * 自定义任务配置
         */
        private ScheduleConfig custom;

        /**
         * 驱动调度任务配置
         *
         * @author pnoker
         * @version 2025.6.0
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
