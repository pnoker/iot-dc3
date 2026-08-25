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

package io.github.pnoker.common.driver.entity.property;

import io.github.pnoker.common.driver.entity.dto.CommandAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.DriverAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.EventAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeDTO;
import io.github.pnoker.common.enums.DriverTypeEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Spring configuration properties for a driver instance.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "dc3.driver")
public class DriverProperties {

    /**
     * Tenant name of the driver instance.
     */
    @NotBlank(message = "Tenant can't be empty")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$", message = "Invalid tenant")
    private String tenant;

    /**
     * Driver runtime type.
     */
    @NotNull(message = "Driver type can't be empty")
    private DriverTypeEnum type = DriverTypeEnum.DRIVER_CLIENT;

    /**
     * Driver display name.
     */
    @NotBlank(message = "Driver name can't be empty")
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5\\s_#@/.|\\-]{1,63}$", message = "Invalid driver name format")
    private String name;

    /**
     * Driver code defined in configuration.
     */
    @NotBlank(message = "Driver code can't be empty")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$", message = "Invalid driver code")
    private String code;

    /**
     * Description
     */
    private String remark;

    /**
     * Schedule configuration for periodic driver tasks.
     */
    @Valid
    @NotNull(message = "Driver schedule configuration can't be empty")
    private ScheduleProperties schedule = new ScheduleProperties();

    /**
     * Health configuration for driver-side runtime checks.
     */
    @Valid
    @NotNull(message = "Driver health configuration can't be empty")
    private HealthProperties health = new HealthProperties();

    /**
     * Local buffer configuration for point-value resume on broker outage.
     */
    @Valid
    @NotNull(message = "Driver buffer configuration can't be empty")
    private BufferProperties buffer = new BufferProperties();

    /**
     * Metadata cache tuning for the driver runtime.
     */
    @Valid
    @NotNull(message = "Driver metadata configuration can't be empty")
    private MetadataProperties metadata = new MetadataProperties();

    /**
     * Distributed runtime lease and heartbeat configuration.
     */
    @Valid
    @NotNull(message = "Driver lease configuration can't be empty")
    private LeaseProperties lease = new LeaseProperties();

    /**
     * Driver-level attribute definitions declared in configuration.
     */
    private List<@Valid DriverAttributeDTO> driverAttribute;

    /**
     * Point-level attribute definitions declared in configuration.
     */
    private List<@Valid PointAttributeDTO> pointAttribute;

    /**
     * Command-level attribute definitions declared in configuration.
     */
    private List<@Valid CommandAttributeDTO> commandAttribute;

    /**
     * Event-level attribute definitions declared in configuration.
     */
    private List<@Valid EventAttributeDTO> eventAttribute;

    /**
     * Generated or configured driver node identifier.
     */
    @NotBlank(message = "Driver node can't be empty")
    private String node;

    /**
     * Driver service name, typically composed from tenant and application name.
     */
    @NotBlank(message = "Driver service can't be empty")
    private String service;

    /**
     * Host address exposed by the driver process.
     */
    @NotBlank(message = "Driver host can't be empty")
    private String host;

    /**
     * Service port exposed by the driver process.
     */
    private Integer port;

    /**
     * Driver client identifier used for queue and registration routing.
     */
    @NotBlank(message = "Driver client can't be empty")
    private String client;

    /**
     * Tunables for the driver-side metadata caches (device/point).
     */
    @Getter
    @Setter
    public static class MetadataProperties {

        /**
         * Caffeine cache settings for driver-side metadata lookups.
         */
        @Valid
        private CacheProperties cache = new CacheProperties();

        /**
         * Caffeine-backed metadata cache tuning (size, lookup timeout, stats).
         */
        @Getter
        @Setter
        public static class CacheProperties {

            /**
             * Maximum number of entries kept in each metadata cache. Eviction is
             * size-based; freshness comes from RabbitMQ events, not TTL.
             */
            @Min(1)
            private long maxSize = 5000L;

            /**
             * Upper bound on a single cache lookup. Expired waits return {@code null}
             * so a stuck manager center cannot pin Quartz/RabbitMQ worker threads.
             */
            @Min(1)
            private long loadTimeoutSeconds = 5L;

            /**
             * Whether Caffeine should record hit/miss/eviction statistics. Off by
             * default — flip on when diagnosing cache behavior in production.
             */
            private boolean recordStats = false;

        }

    }

    /**
     * Health reporting options.
     */
    @Getter
    @Setter
    public static class HealthProperties {

        /**
         * Device health lease configuration.
         */
        @Valid
        private DeviceHealthProperties device = new DeviceHealthProperties();

    }

    /**
     * Device health reporting options.
     */
    @Getter
    @Setter
    public static class DeviceHealthProperties {

        /**
         * Whether the SDK should periodically evaluate and report device health.
         */
        private Boolean enabled = true;

        /**
         * Quartz cron expression used by the device health job. Drivers can tune
         * this separately from the fixed driver heartbeat cadence.
         */
        private String cron = "0/15 * * * * ?";

        /**
         * Fallback device state lease duration. Protocol drivers can return a
         * per-device timeout from the health hook when different devices need
         * different offline windows.
         */
        @Min(1)
        private int timeout = 45;

        /**
         * Unit for the fallback device state lease duration.
         */
        @NotNull
        private TimeUnit timeoutUnit = TimeUnit.SECONDS;

    }

    /**
     * Scheduling options for built-in driver jobs.
     */
    @Getter
    @Setter
    public static class ScheduleProperties {

        /**
         * Periodic read job configuration.
         */
        @Valid
        private ScheduleConfig read = new ScheduleConfig();

        /**
         * Custom job configuration.
         */
        @Valid
        private ScheduleConfig custom = new ScheduleConfig();

        /**
         * Cron-based schedule definition.
         */
        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ScheduleConfig {

            /**
             * Whether the job is enabled.
             */
            private Boolean enabled = false;

            /**
             * Quartz cron expression used by the job. Defaults to every 15 minutes on
             * the 0th second; the previous default fired every second whenever the
             * minute was a multiple of 15, which let read scans pile up well past the
             * SDK's intent.
             */
            private String cron = "0 */15 * * * ?";

        }

    }

    /**
     * Local point-value buffer options. Failed/NACKed point values are persisted to an
     * embedded SQLite database and republished by a Quartz job once the broker recovers,
     * so a RabbitMQ outage no longer loses collected readings.
     */
    @Getter
    @Setter
    public static class BufferProperties {

        /**
         * SQLite database path, relative to the driver working directory. Mirrors the
         * {@code dc3/logs} layout so each driver writes its own buffer file.
         */
        @NotBlank(message = "Buffer db path can't be empty")
        private String dbPath = "dc3/data/driver/buffer.db";

        /**
         * Number of buffered point values republished per Quartz tick.
         */
        @Min(1)
        private int batchSize = 200;

        /**
         * Quartz cron expression for the buffer republish job.
         */
        @NotBlank(message = "Buffer republish cron can't be empty")
        private String republishCron = "0/10 * * * * ?";

        /**
         * Initial backoff before the first republish retry, in seconds.
         */
        @Min(1)
        private long backoffSeconds = 10;

        /**
         * Upper bound the doubling backoff is capped at, in seconds.
         */
        @Min(1)
        private long maxBackoffSeconds = 600;

    }

    /**
     * Runtime membership lease. A node stops processing devices as soon as this lease
     * expires locally, even if Manager Center is unavailable.
     */
    @Getter
    @Setter
    public static class LeaseProperties {

        @Min(10)
        @Max(120)
        private int seconds = 30;

        @NotBlank(message = "Lease renewal cron can't be empty")
        private String renewCron = "0/10 * * * * ?";

        /**
         * Delete per-instance command queues after a departed node remains unused.
         */
        @Min(60000)
        private int queueExpiresMillis = 300000;
    }

}
