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

package io.github.pnoker.common.driver.entity.bo;

import io.github.pnoker.common.driver.entity.dto.DriverAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.CommandAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.EventAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Registration payload used when a driver instance announces itself to the manager
 * center.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RegisterBO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Tenant name of the driver instance.
     */
    private String tenant;

    /**
     * Driver client identifier.
     */
    private String client;

    /**
     * Driver definition to register.
     */
    private DriverBO driver;

    /**
     * Driver-level attribute definitions.
     */
    private List<DriverAttributeDTO> driverAttributes;

    /**
     * Point-level attribute definitions.
     */
    private List<PointAttributeDTO> pointAttributes;

    /**
     * Command-level attribute definitions.
     */
    private List<CommandAttributeDTO> commandAttributes;

    /**
     * Event-level attribute definitions.
     */
    private List<EventAttributeDTO> eventAttributes;

}
