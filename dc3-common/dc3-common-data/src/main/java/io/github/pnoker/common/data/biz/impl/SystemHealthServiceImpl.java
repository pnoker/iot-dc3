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

package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.SystemHealthService;
import io.github.pnoker.common.data.cache.LocalCacheImpl;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.data.entity.vo.dashboard.SystemHealthVO;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.DefaultFlagEnum;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.TenantFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import io.github.pnoker.common.facade.entity.query.FacadeDriverQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Probes each dependency with a bounded, catch-all wrapper so a single flaky dependency
 * doesn't break the whole snapshot. Each probe returns "up" or "down" — ambiguous states
 * are treated as "down".
 *
 * <p>
 * Driver / device summaries have to live in this service: the online-status cache in
 * {@link LocalCacheImpl} is populated by the heartbeat receivers here, not in manager.
 * The fleet facades are called with a tenantId so the manager-side query filters to the
 * caller's tenant (leaving it null routes through gRPC as 0 and matches no rows).
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemHealthServiceImpl implements SystemHealthService {

    /**
     * Per-probe hard deadline. gRPC facades don't set a client-side timeout by default,
     * so a dead sibling would otherwise stall the whole banner.
     */
    private static final long PROBE_TIMEOUT_MS = 2000L;
    private static final String CENTER_AUTH = "auth";
    private static final String CENTER_DATA = "data";
    private static final String CENTER_MANAGER = "manager";
    private static final String INFRA_DATABASE = "database";
    private static final String INFRA_MQ = "mq";
    private static final String INFRA_GATEWAY = "gateway";
    private final DataSource dataSource;
    private final ConnectionFactory rabbitConnectionFactory;
    private final TenantFacade tenantFacade;
    private final DriverFacade driverFacade;
    private final DeviceFacade deviceFacade;
    private final EntityStateManager entityStateManager;

    private static Pages firstPage(int size) {
        Pages p = new Pages();
        p.setCurrent(1L);
        p.setSize((long) size);
        return p;
    }

    /**
     * Runs a truthy probe on the common ForkJoinPool with a hard timeout. Any exception,
     * false return, or timeout maps to "down".
     */
    private static String probe(Probe probe) {
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                return probe.check();
            } catch (Exception e) {
                log.debug("Probe failed", e);
                return false;
            }
        });
        try {
            return future.get(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS) ? DataConstant.Health.STATUS_UP
                    : DataConstant.Health.STATUS_DOWN;
        } catch (TimeoutException ignored) {
            future.cancel(true);
            log.debug("Probe timed out after {}ms", PROBE_TIMEOUT_MS);
            return DataConstant.Health.STATUS_DOWN;
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return DataConstant.Health.STATUS_DOWN;
        } catch (ExecutionException e) {
            log.debug("Probe execution failed", e);
            return DataConstant.Health.STATUS_DOWN;
        }
    }

    @Override
    public SystemHealthVO snapshot(Long tenantId) {
        SystemHealthVO vo = new SystemHealthVO();
        vo.setCenter(probeCenter());
        vo.setInfra(probeInfra());
        vo.setDrivers(summariseDrivers(tenantId));
        vo.setDevices(summariseDevices(tenantId));
        return vo;
    }

    /**
     * Probe the reachability of each center service: data is up by definition (this code
     * runs there), auth and manager are probed through their facades with a timeout.
     *
     * @return map from center name to its up/down status code
     */
    private Map<String, String> probeCenter() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put(CENTER_AUTH, probe(() -> Objects.nonNull(tenantFacade.getByCode(DefaultFlagEnum.DEFAULT.getCode()))));
        out.put(CENTER_DATA, DataConstant.Health.STATUS_UP); // reaching this code == data is up
        out.put(CENTER_MANAGER, probe(() -> {
            FacadeDriverQuery q = FacadeDriverQuery.builder().page(firstPage(1)).build();
            return Objects.nonNull(driverFacade.listByPage(q));
        }));
        return out;
    }

    /**
     * Probe the reachability of infrastructure dependencies: database via a test
     * connection, message queue via a test connection, gateway by inference (the request
     * reached this server through it).
     *
     * @return map from infra name to its up/down status code
     */
    private Map<String, String> probeInfra() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put(INFRA_DATABASE, probe(() -> {
            try (Connection conn = dataSource.getConnection()) {
                return conn.isValid(1);
            }
        }));
        out.put(INFRA_MQ, probe(() -> {
            try (var connection = rabbitConnectionFactory.createConnection()) {
                return connection.isOpen();
            }
        }));
        out.put(INFRA_GATEWAY, DataConstant.Health.STATUS_UP); // the request reached this server via gateway
        return out;
    }

    /**
     * Summarize the driver fleet for a tenant: total count and how many are currently
     * online. The driver page is fetched asynchronously with a timeout, then each
     * driver's entity state is checked against its lease expiry.
     *
     * @param tenantId tenant scope
     * @return fleet summary with total and online driver counts
     */
    private SystemHealthVO.FleetSummary summariseDrivers(Long tenantId) {
        SystemHealthVO.FleetSummary summary = new SystemHealthVO.FleetSummary();
        CompletableFuture<List<FacadeDriverBO>> future = CompletableFuture.supplyAsync(() -> {
            FacadeDriverQuery q = FacadeDriverQuery.builder().page(firstPage(1000)).tenantId(tenantId).build();
            FacadePage<FacadeDriverBO> page = driverFacade.listByPage(q);
            return Objects.nonNull(page) ? page.getRecords() : List.<FacadeDriverBO>of();
        });
        List<FacadeDriverBO> drivers;
        try {
            drivers = future.get(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Driver summary interrupted, tenantId={}", tenantId, e);
            return summary;
        } catch (Exception e) {
            future.cancel(true);
            log.warn("Driver summary failed, tenantId={}", tenantId, e);
            return summary;
        }
        int online = 0;
        LocalDateTime now = LocalDateTime.now();
        for (FacadeDriverBO d : drivers) {
            EntityStateDO state = entityStateManager.lambdaQuery()
                    .eq(EntityStateDO::getTenantId, tenantId)
                    .eq(EntityStateDO::getEntityTypeFlag, EntityTypeEnum.DRIVER.getIndex())
                    .eq(EntityStateDO::getEntityId, d.getId())
                    .one();
            if (Objects.nonNull(state) && !state.getExpireTime().isBefore(now)) {
                EntityStatusEnum e = EntityStatusEnum.ofIndex(state.getStateFlag());
                if (Objects.nonNull(e) && EntityStatusEnum.ONLINE.getCode().equals(e.getCode())) {
                    online++;
                }
            }
        }
        summary.setTotal(drivers.size());
        summary.setOnline(online);
        return summary;
    }

    /**
     * Summarize the device fleet for a tenant: total count and how many are currently
     * online. Structured like {@link #summariseDrivers} but scoped to devices.
     *
     * @param tenantId tenant scope
     * @return fleet summary with total and online device counts
     */
    private SystemHealthVO.FleetSummary summariseDevices(Long tenantId) {
        SystemHealthVO.FleetSummary summary = new SystemHealthVO.FleetSummary();
        CompletableFuture<List<FacadeDeviceBO>> future = CompletableFuture.supplyAsync(() -> {
            FacadeDeviceQuery q = FacadeDeviceQuery.builder().page(firstPage(5000)).tenantId(tenantId).build();
            FacadePage<FacadeDeviceBO> page = deviceFacade.listByPage(q);
            return Objects.nonNull(page) ? page.getRecords() : List.<FacadeDeviceBO>of();
        });
        List<FacadeDeviceBO> devices;
        try {
            devices = future.get(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Device summary interrupted, tenantId={}", tenantId, e);
            return summary;
        } catch (Exception e) {
            future.cancel(true);
            log.warn("Device summary failed, tenantId={}", tenantId, e);
            return summary;
        }
        int online = 0;
        LocalDateTime now = LocalDateTime.now();
        for (FacadeDeviceBO d : devices) {
            EntityStateDO state = entityStateManager.lambdaQuery()
                    .eq(EntityStateDO::getTenantId, tenantId)
                    .eq(EntityStateDO::getEntityTypeFlag, EntityTypeEnum.DEVICE.getIndex())
                    .eq(EntityStateDO::getEntityId, d.getId())
                    .one();
            if (Objects.nonNull(state) && !state.getExpireTime().isBefore(now)) {
                EntityStatusEnum e = EntityStatusEnum.ofIndex(state.getStateFlag());
                if (Objects.nonNull(e) && EntityStatusEnum.ONLINE.getCode().equals(e.getCode())) {
                    online++;
                }
            }
        }
        summary.setTotal(devices.size());
        summary.setOnline(online);
        return summary;
    }

    @FunctionalInterface
    private interface Probe {

        boolean check() throws Exception;

    }

}
