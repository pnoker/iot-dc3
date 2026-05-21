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

package io.github.pnoker.common.driver.job;

import io.github.pnoker.common.driver.entity.bo.DriverBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.DriverStateDTO;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that periodically evaluates and reports driver connectivity status.
 *
  * @author pnoker
  * @version 2025.9.0
  * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DriverStatusScheduleJob extends QuartzJobBean {

    private final DriverMetadata driverMetadata;

    private final DriverSenderService driverSenderService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        DriverBO driver = driverMetadata.getDriver();
        DriverStateDTO driverState = new DriverStateDTO(driver.getId(), driverMetadata.getDriverStatus().getCode());
        driverState.setTenantId(driver.getTenantId());
        log.info("Report driver state: {}", JsonUtil.toJsonString(driverState));
        driverSenderService.driverStateSender(driverState);
    }

}
