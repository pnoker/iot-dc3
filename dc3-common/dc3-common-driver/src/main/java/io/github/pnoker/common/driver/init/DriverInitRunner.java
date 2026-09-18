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

import io.github.pnoker.common.driver.buffer.BufferService;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverRegisterService;
import io.github.pnoker.common.driver.service.DriverScheduleService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

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
 * @since 2016.10.1
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@ComponentScan(basePackages = {"io.github.pnoker.common.driver"})
@EnableConfigurationProperties({DriverProperties.class})
public class DriverInitRunner implements ApplicationRunner {

    /**
     * Maximum number of driver registration attempts before giving up.
     */
    private static final int REGISTER_MAX_ATTEMPTS = 30;
    /**
     * Initial backoff delay before the first registration retry.
     */
    private static final Duration REGISTER_INITIAL_BACKOFF = Duration.ofSeconds(2);
    /**
     * Upper bound the doubling backoff delay is capped at.
     */
    private static final Duration REGISTER_MAX_BACKOFF = Duration.ofSeconds(30);

    private final DriverRegisterService driverRegisterService;

    private final DriverCustomService driverCustomService;

    private final DriverScheduleService driverScheduleService;

    /**
     * Local point-value buffer, initialized before registration so readings survive a manager outage.
     */
    private final BufferService bufferService;

    /**
     * Runs the driver bootstrap sequence on startup: register with the manager center
     * (with retry), execute custom initialization, then initialize scheduled tasks.
     *
     * @param args application arguments
     * @throws Exception if registration ultimately fails or initialization errors out
     */
    @Override
    public void run(ApplicationArguments args) {
        Mono.fromRunnable(bufferService::initialize)
                .then(registerWithRetry())
                .then(Mono.fromRunnable(driverCustomService::initial))
                .then(Mono.fromRunnable(driverScheduleService::initialize))
                .doOnSuccess(ignored -> log.info("Driver runtime initialized"))
                .doOnError(error -> log.error("Driver runtime initialization failed", error))
                .subscribe();
    }

    /**
     * Register the driver with the manager center, retrying with exponential backoff
     * (doubling up to a cap) until the max attempt count is reached.
     *
     * @throws InterruptedException if the backoff sleep is interrupted
     */
    private Mono<Void> registerWithRetry() {
        return driverRegisterService
                .initial()
                .retryWhen(Retry.backoff(REGISTER_MAX_ATTEMPTS - 1, REGISTER_INITIAL_BACKOFF)
                        .maxBackoff(REGISTER_MAX_BACKOFF)
                        .doBeforeRetry(signal -> log.warn(
                                "Driver registration failed, attempt={}, maxAttempts={}, retryDelayMillis={}",
                                signal.totalRetries() + 1,
                                REGISTER_MAX_ATTEMPTS,
                                Math.min(
                                        REGISTER_INITIAL_BACKOFF.toMillis()
                                                * (1L << Math.min(signal.totalRetries(), 30)),
                                        REGISTER_MAX_BACKOFF.toMillis()),
                                signal.failure()))
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }
}
