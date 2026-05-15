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

package io.github.pnoker.common.config;

import io.github.pnoker.common.quartz.QuartzService;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuartzConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(QuartzConfig.class));

    @Test
    void quartzServiceIsCreatedWhenSchedulerExists() {
        Scheduler scheduler = mock(Scheduler.class);

        contextRunner.withBean(Scheduler.class, () -> scheduler)
                .run(context -> {
                    when(scheduler.isShutdown()).thenReturn(true);

                    context.getBean(QuartzService.class).startScheduler();

                    assertThat(context).hasSingleBean(QuartzService.class);
                    verify(scheduler).isShutdown();
                });
    }

    @Test
    void quartzServiceIsSkippedWhenSchedulerMissing() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(QuartzService.class));
    }

    @Test
    void quartzServiceBacksOffWhenUserBeanExists() {
        Scheduler scheduler = mock(Scheduler.class);
        QuartzService customService = new QuartzService(scheduler);

        contextRunner.withBean(Scheduler.class, () -> scheduler)
                .withBean(QuartzService.class, () -> customService)
                .run(context -> assertThat(context.getBean(QuartzService.class)).isSameAs(customService));
    }

}
