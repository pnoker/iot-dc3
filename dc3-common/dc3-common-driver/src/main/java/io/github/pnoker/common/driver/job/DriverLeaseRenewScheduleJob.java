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

import io.github.pnoker.common.driver.grpc.client.DriverClient;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

/**
 * Renews runtime membership. Expired local leases automatically stop device work.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class DriverLeaseRenewScheduleJob extends QuartzJobBean {

    private final DriverClient driverClient;
    private final DriverMetadata driverMetadata;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        try {
            driverClient.renewLease();
        } catch (Exception e) {
            log.error("Driver lease renewal failed, leaseValid={}, leaseUntilEpochMillis={}",
                    driverMetadata.leaseValid(), driverMetadata.getLeaseUntilEpochMillis(), e);
        }
    }
}
