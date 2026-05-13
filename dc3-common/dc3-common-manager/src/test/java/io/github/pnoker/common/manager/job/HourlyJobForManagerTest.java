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

package io.github.pnoker.common.manager.job;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;

import static org.assertj.core.api.Assertions.assertThatNoException;

class HourlyJobForManagerTest {

    @Test
    void executeInternalCompletesWithoutThrowing() {
        HourlyJobForManager job = new HourlyJobForManager();
        JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        assertThatNoException().isThrownBy(() -> job.executeInternal(context));
    }

    @Test
    void executeInternalToleratesNullContext() {
        HourlyJobForManager job = new HourlyJobForManager();
        // The hourly heartbeat does not read the context — the test pins that
        // contract so future contributors do not silently start depending on
        // ctx.getMergedJobDataMap and break when invoked from a non-Quartz path.
        assertThatNoException().isThrownBy(() -> job.executeInternal(null));
    }
}
