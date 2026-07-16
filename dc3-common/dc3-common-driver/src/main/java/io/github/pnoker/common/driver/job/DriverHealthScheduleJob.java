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

import io.github.pnoker.common.driver.entity.bean.DriverHealthState;
import io.github.pnoker.common.driver.entity.bo.DriverBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.DriverStateDTO;
import io.github.pnoker.common.enums.EntityStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Scheduled job that periodically evaluates and reports driver health.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class DriverHealthScheduleJob extends QuartzJobBean {

    private final DriverMetadata driverMetadata;

    private final DriverCustomService driverCustomService;

    private final DriverSenderService driverSenderService;

    /**
     * Evaluates the current driver health and reports its state to the platform on each scheduled trigger.
     */
    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        DriverBO driver = driverMetadata.getDriver();
        if (driver == null) {
            log.warn("Skip driver state report: driver metadata is not initialized");
            return;
        }

        DriverHealthState healthState = resolveDriverHealth();
        EntityStatusEnum status = healthState.getStatus();
        driverMetadata.setDriverStatus(status);

        DriverStateDTO driverState = new DriverStateDTO(driver.getId(), status);
        driverState.setTenantId(driver.getTenantId());
        if (Objects.nonNull(healthState.getDescription())) {
            driverState.setStateDescription(healthState.getDescription());
        }
        log.debug("Report driver health: driverId={}, status={}", driverState.getDriverId(), driverState.getStatus());
        driverSenderService.driverStateSender(driverState);
    }

    /**
     * Resolve the driver's health state via the driver health check, defaulting to FAULT
     * when the check fails or returns no status.
     *
     * @return the resolved driver health state
     */
    private DriverHealthState resolveDriverHealth() {
        try {
            DriverHealthState healthState = driverCustomService.health();
            if (Objects.nonNull(healthState) && Objects.nonNull(healthState.getStatus())) {
                return healthState;
            }
        } catch (Exception e) {
            log.warn("Driver health check failed", e);
        }
        return DriverHealthState.fault();
    }

}
