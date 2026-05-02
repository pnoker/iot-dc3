/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.data.biz.SystemHealthService;
import io.github.pnoker.common.data.cache.LocalCacheService;
import io.github.pnoker.common.data.entity.vo.dashboard.SystemHealthVO;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.TenantFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDriverQuery;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Probes each dependency with a bounded, catch-all wrapper so a single flaky
 * dependency doesn't break the whole snapshot. Each probe returns "up" or
 * "down" — ambiguous states are treated as "down" to err on the alarming side.
 *
 * <p>Why this lives in dc3-common-data instead of auth: the driver
 * online/offline state is only cached in this service's {@link LocalCacheService},
 * and walking out to fetch it from a sibling would defeat the whole "one
 * aggregated probe" point.</p>
 *
 * @author pnoker
 * @since 2026.5.2
 */
@Slf4j
@Service
public class SystemHealthServiceImpl implements SystemHealthService {

    private static final String UP = "up";
    private static final String DOWN = "down";

    /**
     * Per-probe hard deadline. gRPC facades don't set a client-side timeout
     * by default, so a dead sibling would otherwise stall the whole banner.
     */
    private static final long PROBE_TIMEOUT_MS = 2000L;

    @Resource
    private DataSource dataSource;

    @Resource
    private ConnectionFactory rabbitConnectionFactory;

    @Resource
    private TenantFacade tenantFacade;

    @Resource
    private DriverFacade driverFacade;

    @Resource
    private LocalCacheService localCacheService;

    @Override
    public SystemHealthVO snapshot() {
        SystemHealthVO vo = new SystemHealthVO();
        vo.setCenter(probeCenter());
        vo.setInfra(probeInfra());
        vo.setDrivers(summariseDrivers());
        return vo;
    }

    private Map<String, String> probeCenter() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("auth", probe(() -> tenantFacade.selectByCode("default") != null));
        out.put("data", UP); // reaching this code == data is up
        out.put("manager", probe(() -> {
            FacadeDriverQuery q = FacadeDriverQuery.builder().page(firstPage(1)).build();
            return driverFacade.selectByPage(q) != null;
        }));
        return out;
    }

    private Map<String, String> probeInfra() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("database", probe(() -> {
            try (Connection conn = dataSource.getConnection()) {
                return conn.isValid(1);
            }
        }));
        out.put("mq", probe(() -> {
            try (var connection = rabbitConnectionFactory.createConnection()) {
                return connection.isOpen();
            }
        }));
        out.put("gateway", UP); // the request reached this server via gateway
        return out;
    }

    private SystemHealthVO.DriverSummary summariseDrivers() {
        SystemHealthVO.DriverSummary summary = new SystemHealthVO.DriverSummary();
        CompletableFuture<List<FacadeDriverBO>> future = CompletableFuture.supplyAsync(() -> {
            FacadeDriverQuery q = FacadeDriverQuery.builder().page(firstPage(1000)).build();
            FacadePage<FacadeDriverBO> page = driverFacade.selectByPage(q);
            return page != null ? page.getRecords() : List.<FacadeDriverBO>of();
        });
        List<FacadeDriverBO> drivers;
        try {
            drivers = future.get(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            future.cancel(true);
            log.warn("Driver summary failed: {}", e.getMessage(), e);
            return summary;
        }
        int online = 0;
        int missingCache = 0;
        String onlineCode = DriverStatusEnum.ONLINE.getCode();
        for (FacadeDriverBO d : drivers) {
            String key = PrefixConstant.DRIVER_STATUS_KEY_PREFIX + d.getId();
            String status = localCacheService.getKey(key);
            if (status == null) missingCache++;
            if (Objects.equals(status, onlineCode)) {
                online++;
            }
        }
        summary.setTotal(drivers.size());
        summary.setOnline(online);
        // Surfaces the common failure modes without needing to bump the log
        // level: either the facade returned zero drivers (gRPC mis-wired or
        // tenant filter too aggressive) or the status cache is cold (no
        // heartbeat has reached data since startup).
        log.info("System health drivers: total={}, online={}, missingStatusCache={}",
                drivers.size(), online, missingCache);
        return summary;
    }

    private static Pages firstPage(int size) {
        Pages p = new Pages();
        p.setCurrent(1L);
        p.setSize((long) size);
        return p;
    }

    /**
     * Runs a truthy probe on the common ForkJoinPool with a hard timeout.
     * Any exception, false return, or timeout maps to "down".
     */
    private static String probe(Probe probe) {
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                return probe.check();
            } catch (Exception e) {
                log.debug("Probe failed: {}", e.getMessage());
                return false;
            }
        });
        try {
            return future.get(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS) ? UP : DOWN;
        } catch (TimeoutException e) {
            future.cancel(true);
            log.debug("Probe timed out after {}ms", PROBE_TIMEOUT_MS);
            return DOWN;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return DOWN;
        } catch (ExecutionException e) {
            log.debug("Probe execution failed: {}", e.getMessage());
            return DOWN;
        }
    }

    @FunctionalInterface
    private interface Probe {
        boolean check() throws Exception;
    }
}
