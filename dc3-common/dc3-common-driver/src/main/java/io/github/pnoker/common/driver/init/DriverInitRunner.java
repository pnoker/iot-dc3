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

package io.github.pnoker.common.driver.init;

import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverRegisterService;
import io.github.pnoker.common.driver.service.DriverScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import java.time.Duration;

/**
 * Application startup runner that completes the standard driver bootstrap sequence:
 * registration, custom initialization, and schedule initialization.
 *
 * <p>Driver registration goes through the manager center over gRPC. Manager may not be
 * ready when the driver process starts (rolling restart, K8s pod reschedule), so the
 * register call retries with capped exponential backoff before giving up — without it
 * a transient outage cascades into a full driver CrashLoopBackOff.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@ComponentScan(basePackages = {"io.github.pnoker.common.driver"})
@EnableConfigurationProperties({DriverProperties.class})
public class DriverInitRunner implements ApplicationRunner {

    private static final int REGISTER_MAX_ATTEMPTS = 30;
    private static final Duration REGISTER_INITIAL_BACKOFF = Duration.ofSeconds(2);
    private static final Duration REGISTER_MAX_BACKOFF = Duration.ofSeconds(30);

    private final DriverRegisterService driverRegisterService;

    private final DriverCustomService driverCustomService;

    private final DriverScheduleService driverScheduleService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Initialize driver registration and synchronize basic information with the
        // platform; tolerate manager center being temporarily unavailable.
        registerWithRetry();

        // Execute custom initialization functions specific to this driver module
        driverCustomService.initial();

        // Initialize driver tasks including status monitoring, reading operations and
        // custom tasks
        driverScheduleService.initialize();
    }

    private void registerWithRetry() throws InterruptedException {
        long backoffMs = REGISTER_INITIAL_BACKOFF.toMillis();
        for (int attempt = 1; attempt <= REGISTER_MAX_ATTEMPTS; attempt++) {
            try {
                driverRegisterService.initial();
                if (attempt > 1) {
                    log.info("Driver register succeeded on attempt {}", attempt);
                }
                return;
            } catch (Exception e) {
                if (attempt == REGISTER_MAX_ATTEMPTS) {
                    log.error("Driver register failed after {} attempts, giving up", attempt, e);
                    throw e;
                }
                log.warn("Driver register failed on attempt {}/{}, retrying in {} ms: {}", attempt,
                        REGISTER_MAX_ATTEMPTS, backoffMs, e.getMessage());
                Thread.sleep(backoffMs);
                backoffMs = Math.min(backoffMs * 2, REGISTER_MAX_BACKOFF.toMillis());
            }
        }
    }

}
