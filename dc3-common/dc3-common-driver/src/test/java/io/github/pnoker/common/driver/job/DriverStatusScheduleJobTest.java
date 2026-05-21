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
import io.github.pnoker.common.enums.DriverStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DriverStatusScheduleJobTest {

    @Mock
    private DriverSenderService driverSenderService;

    @Mock
    private JobExecutionContext jobContext;

    private DriverMetadata driverMetadata;
    private DriverStatusScheduleJob job;

    @BeforeEach
    void setUp() {
        driverMetadata = new DriverMetadata();
        job = new DriverStatusScheduleJob(driverMetadata, driverSenderService);
    }

    @Test
    void skipsReportWhenDriverIsNotInitialized() {
        // driver remains null — typically the case during a DRIVER DELETE clear or
        // before initial registration completes.
        assertThatNoException().isThrownBy(() -> job.executeInternal(jobContext));
        verifyNoInteractions(driverSenderService);
    }

    @Test
    void reportsCurrentDriverStateWhenInitialized() {
        DriverBO driver = new DriverBO();
        driver.setId(42L);
        driver.setTenantId(7L);
        driverMetadata.setDriver(driver);
        driverMetadata.setDriverStatus(DriverStatusEnum.ONLINE);

        job.executeInternal(jobContext);

        ArgumentCaptor<DriverStateDTO> captor = ArgumentCaptor.forClass(DriverStateDTO.class);
        verify(driverSenderService).driverStateSender(captor.capture());
        DriverStateDTO sent = captor.getValue();
        assertThat(sent.getDriverId()).isEqualTo(42L);
        assertThat(sent.getTenantId()).isEqualTo(7L);
        assertThat(sent.getStatus()).isEqualTo(DriverStatusEnum.ONLINE.getCode());
    }

}
