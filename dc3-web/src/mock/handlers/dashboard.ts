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

import { on } from "../dispatch";
import {
  localizeAlertRows,
  localizeStreamRows,
  localizeTopology,
} from "../locale";
import { ok, okPage, responseOf } from "../response";
import {
  alertAging,
  alertStats,
  dailyGrowth,
  deviceStats,
  driverStats,
  silentSources,
  statsActivity,
  statsLatency,
  statsTimeseries,
  statsToday,
  statsTop,
  streamLatest,
  systemHealth,
  topology,
} from "../seed/dashboard";
import type { MockAlertRow } from "../seed/alarm";
import {
  alertActivityRows,
  alertChangeImpactRows,
  alertCorrelationRows,
  alertFlappingRows,
  alertMttaRows,
  alertPeerDeviationRows,
  alertRecords,
  alertStormRows,
  alertTopSourceRows,
  alertTrendRows,
  alertTypeRows,
  coverageGapReport,
  protocolHealthRows,
} from "../seed/alarm";

const confirmationOverrides = new Map<string, MockAlertRow["confirmFlag"]>();

const withConfirmationOverrides = (rows: MockAlertRow[]): MockAlertRow[] =>
  rows.map((row) => ({
    ...row,
    confirmFlag: confirmationOverrides.get(row.id) ?? row.confirmFlag,
  }));

const rangeStart = (
  body: Record<string, any>,
  now = new Date(),
): number | null => {
  const rangeKey = String(body.rangeKey ?? "");
  if (rangeKey === "today") {
    const start = new Date(now);
    start.setHours(0, 0, 0, 0);
    return start.getTime();
  }
  const hours =
    rangeKey === "24h"
      ? 24
      : rangeKey === "7d"
        ? 24 * 7
        : rangeKey === "30d"
          ? 24 * 30
          : Number(body.rangeHours);
  return Number.isFinite(hours) && hours > 0
    ? now.getTime() - hours * 3_600_000
    : null;
};

const filteredAlerts = (body: Record<string, any>): MockAlertRow[] => {
  const from = rangeStart(body);
  return withConfirmationOverrides(alertRecords()).filter((row) => {
    if (body.source && row.source !== body.source) return false;
    if (
      body.eventTypeFlag !== null &&
      body.eventTypeFlag !== undefined &&
      body.eventTypeFlag !== ""
    ) {
      if (row.eventTypeFlag !== Number(body.eventTypeFlag)) return false;
    }
    if (
      body.confirmFlag !== null &&
      body.confirmFlag !== undefined &&
      body.confirmFlag !== ""
    ) {
      const expected =
        Number(body.confirmFlag) === 1 ? "CONFIRMED" : "UNCONFIRMED";
      if (row.confirmFlag !== expected) return false;
    }
    if (from !== null) {
      const timestamp = new Date(row.createTime.replace(" ", "T")).getTime();
      if (!Number.isFinite(timestamp) || timestamp < from) return false;
    }
    return true;
  });
};

/**
 * Home page hard dependencies plus the insight-card endpoints that populate
 * the five secondary cards (live feed, analytics tabs, SLA badge, latency,
 * activity heatmap). The remaining event-overview-only endpoints
 * (alert/trend, flapping, correlation, …) are not registered here — every card
 * wraps its call in try/catch, so the generic fallback keeps them blank.
 */
export function registerDashboardHandlers(): void {
  on("get", "api/v3/data/dashboard/stats/today", (ctx) =>
    responseOf(ctx.config, ok(statsToday)),
  );
  on("get", "api/v3/data/dashboard/stats/timeseries", (ctx) =>
    responseOf(
      ctx.config,
      ok(
        statsTimeseries(
          String(ctx.params.range_key ?? "24h"),
          ctx.params.granularity === "day" ? "day" : "hour",
        ),
      ),
    ),
  );
  on("get", "api/v3/data/dashboard/alert/stats", (ctx) =>
    responseOf(ctx.config, ok(alertStats)),
  );
  on("get", "api/v3/data/dashboard/alert/latest", (ctx) => {
    const size = Math.max(1, Number(ctx.params.size) || 10);
    const rows = withConfirmationOverrides(alertRecords()).slice(0, size);
    return responseOf(ctx.config, ok(localizeAlertRows(rows)));
  });
  on("get", "api/v3/data/dashboard/system/health", (ctx) =>
    responseOf(ctx.config, ok(systemHealth)),
  );
  on("get", "api/v3/manager/dashboard/growth", (ctx) =>
    responseOf(ctx.config, ok(dailyGrowth)),
  );
  on("get", "api/v3/manager/dashboard/topology", (ctx) =>
    responseOf(ctx.config, ok(localizeTopology(topology))),
  );
  on("post", "api/v3/data/dashboard/alert/page", (ctx) => {
    const rows = localizeAlertRows(filteredAlerts(ctx.body));
    const current = Math.max(1, Number(ctx.body.current) || 1);
    const size = Math.max(1, Number(ctx.body.size) || 20);
    const start = (current - 1) * size;
    return responseOf(
      ctx.config,
      okPage(rows.slice(start, start + size), rows.length),
    );
  });
  on("post", "api/v3/data/dashboard/alert/confirm", (ctx) => {
    confirmationOverrides.set(String(ctx.params.id), "CONFIRMED");
    return responseOf(ctx.config, ok(true));
  });
  on("post", "api/v3/data/dashboard/alert/unconfirm", (ctx) => {
    confirmationOverrides.set(String(ctx.params.id), "UNCONFIRMED");
    return responseOf(ctx.config, ok(true));
  });
  on("post", "api/v3/data/dashboard/alert/bulk_confirm", (ctx) => {
    const value: MockAlertRow["confirmFlag"] = ctx.body.confirm
      ? "CONFIRMED"
      : "UNCONFIRMED";
    const items = Array.isArray(ctx.body.items) ? ctx.body.items : [];
    for (const item of items) confirmationOverrides.set(String(item.id), value);
    return responseOf(ctx.config, ok(items.length));
  });

  on("get", "api/v3/data/dashboard/alert/trend", (ctx) =>
    responseOf(ctx.config, ok(alertTrendRows(Number(ctx.params.days) || 30))),
  );
  on("get", "api/v3/data/dashboard/alert/top_sources", (ctx) =>
    responseOf(
      ctx.config,
      ok(alertTopSourceRows(Number(ctx.params.limit) || 10)),
    ),
  );
  on("get", "api/v3/data/dashboard/alert/activity", (ctx) =>
    responseOf(ctx.config, ok(alertActivityRows(Number(ctx.params.days) || 7))),
  );
  on("get", "api/v3/data/dashboard/alert/type_distribution", (ctx) =>
    responseOf(ctx.config, ok(alertTypeRows())),
  );
  on("get", "api/v3/data/dashboard/alert/storm_sources", (ctx) =>
    responseOf(
      ctx.config,
      ok(
        alertStormRows(
          Number(ctx.params.min_count) || 10,
          Number(ctx.params.limit) || 10,
        ),
      ),
    ),
  );
  on("get", "api/v3/data/dashboard/alert/flapping", (ctx) =>
    responseOf(
      ctx.config,
      ok(
        alertFlappingRows(
          Number(ctx.params.min_count) || 5,
          Number(ctx.params.limit) || 20,
        ),
      ),
    ),
  );
  on("get", "api/v3/data/dashboard/alert/correlation", (ctx) =>
    responseOf(
      ctx.config,
      ok(
        alertCorrelationRows(
          Number(ctx.params.hours) || 24,
          Number(ctx.params.limit) || 15,
        ),
      ),
    ),
  );
  on("get", "api/v3/data/dashboard/alert/peer_deviation", (ctx) =>
    responseOf(
      ctx.config,
      ok(alertPeerDeviationRows(Number(ctx.params.days) || 7)),
    ),
  );
  on("get", "api/v3/data/dashboard/alert/mtta", (ctx) =>
    responseOf(ctx.config, ok(alertMttaRows(Number(ctx.params.days) || 30))),
  );
  on("get", "api/v3/data/dashboard/alert/change_impact", (ctx) =>
    responseOf(
      ctx.config,
      ok(
        alertChangeImpactRows(
          Number(ctx.params.days) || 30,
          Number(ctx.params.limit) || 30,
        ),
      ),
    ),
  );
  on("get", "api/v3/data/dashboard/protocol/health", (ctx) =>
    responseOf(ctx.config, ok(protocolHealthRows())),
  );
  on("get", "api/v3/data/dashboard/coverage/gap", (ctx) =>
    responseOf(
      ctx.config,
      ok(coverageGapReport(Number(ctx.params.limit) || 100)),
    ),
  );

  on("get", "api/v3/data/dashboard/stream", (ctx) => {
    const rows = localizeStreamRows(streamLatest());
    const size = Number(ctx.params.size) || rows.length;
    return responseOf(ctx.config, ok(rows.slice(0, size)));
  });
  on("get", "api/v3/manager/dashboard/device/stats", (ctx) =>
    responseOf(ctx.config, ok(deviceStats)),
  );
  on("get", "api/v3/manager/dashboard/driver/stats", (ctx) =>
    responseOf(ctx.config, ok(driverStats)),
  );
  on("get", "api/v3/data/dashboard/top", (ctx) => {
    const dim = (ctx.params.dimension as keyof typeof statsTop) || "device";
    const rows = statsTop[dim] ?? statsTop.device;
    const limit = Number(ctx.params.limit) || 10;
    return responseOf(ctx.config, ok(rows.slice(0, limit)));
  });
  on("get", "api/v3/data/dashboard/alert/aging", (ctx) =>
    responseOf(ctx.config, ok(alertAging)),
  );
  on("get", "api/v3/data/dashboard/silent/sources", (ctx) =>
    responseOf(
      ctx.config,
      ok(
        silentSources()
          .filter(
            (row) =>
              row.silentSeconds >=
              (Number(ctx.params.silent_minutes) || 15) * 60,
          )
          .slice(0, Number(ctx.params.limit) || 50),
      ),
    ),
  );
  on("get", "api/v3/data/dashboard/stats/latency", (ctx) =>
    responseOf(ctx.config, ok(statsLatency)),
  );
  on("get", "api/v3/data/dashboard/stats/activity", (ctx) =>
    responseOf(
      ctx.config,
      ok(statsActivity(String(ctx.params.range_key ?? "7d"))),
    ),
  );
}
