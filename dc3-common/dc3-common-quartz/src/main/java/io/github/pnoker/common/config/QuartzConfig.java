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
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.quartz.autoconfigure.QuartzAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for Quartz scheduled jobs.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.0
 */
@AutoConfiguration(after = QuartzAutoConfiguration.class)
public class QuartzConfig {

    /**
     * Quartz scheduler service.
     *
     * @param scheduler Quartz scheduler
     * @return QuartzService
     */
    @Bean
    @ConditionalOnBean(Scheduler.class)
    @ConditionalOnMissingBean
    public QuartzService quartzService(Scheduler scheduler) {
        return new QuartzService(scheduler);
    }

}
