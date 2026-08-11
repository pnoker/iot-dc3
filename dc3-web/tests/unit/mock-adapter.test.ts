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

import type {AxiosRequestConfig} from 'axios';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import request from '@/config/axios';
import i18n from '@/config/i18n';
import {setupMock} from '@/mock';

vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), warnMessage: vi.fn()}));
vi.mock('@/config/router', () => ({default: {push: vi.fn(() => Promise.resolve())}}));

/**
 * Exercise the mock adapter end-to-end through the real axios instance + its
 * interceptors. Guards the demo's load-bearing shapes: menu tree (router
 * guard), core list pagination, login flow, and the generic fallbacks that
 * keep unregistered endpoints from surfacing errors.
 */
describe('mock adapter', () => {
  beforeEach(() => {
    localStorage.clear();
    i18n.global.locale.value = 'en';
    setupMock();
  });

  // The response interceptor unwraps R<T>, so each call resolves to the R
  // envelope (not an AxiosResponse). Cast once at the boundary.
  const call = (config: AxiosRequestConfig) => request(config) as Promise<R>;

  it('does not pre-seed auth so the demo lands on /login', () => {
    expect(localStorage.getItem('X-Auth-Tenant')).toBeNull();
    expect(localStorage.getItem('X-Auth-Token')).toBeNull();
  });

  it('returns the full menu tree from list_tree', async () => {
    const res = await call({url: 'api/v3/auth/menu/list_tree', method: 'post', data: {}});
    expect(res.ok).toBe(true);
    expect(Array.isArray(res.data)).toBe(true);
    expect(res.data.some((n: any) => n.menuCode === 'driver')).toBe(true);
    expect(res.data.some((n: any) => n.menuCode === 'settings')).toBe(true);
  });

  it('paginates core lists into a PageResult', async () => {
    const res = await call({url: 'api/v3/manager/driver/list', method: 'post', data: {page: {current: 1, size: 12}}});
    expect(res.data.total).toBeGreaterThan(0);
    expect(Array.isArray(res.data.records)).toBe(true);
    expect(res.data.records.length).toBeGreaterThan(0);
  });

  it('resolves get_by_id by id', async () => {
    const res = await call({url: 'api/v3/manager/driver/get_by_id', method: 'get', params: {id: '1001'}});
    expect(res.data.id).toBe('1001');
  });

  it('returns a driver dictionary with label/value', async () => {
    const res = await call({url: 'api/v3/manager/dictionary/driver', method: 'post', data: {}});
    expect(res.data.records[0]).toHaveProperty('label');
    expect(res.data.records[0]).toHaveProperty('value');
  });

  it('mocks the token endpoints so login works end-to-end', async () => {
    const login = {tenant: 'default', name: 'dc3', password: 'dc3dc3dc3'};
    const salt = await call({url: 'api/v3/auth/token/salt', method: 'post', data: login});
    expect(salt.data).toBe('mock-salt');
    const token = await call({url: 'api/v3/auth/token/generate', method: 'post', data: login});
    expect(token.data).toBe('ok');

    await expect(
      call({url: 'api/v3/auth/token/generate', method: 'post', data: {...login, password: 'incorrect'}}),
    ).rejects.toMatchObject({ok: false, code: 'R4010'});
  });

  it('returns the seeded role list with real data', async () => {
    const res = await call({url: 'api/v3/auth/role/list', method: 'post', data: {page: {current: 1, size: 12}}});
    expect(res.ok).toBe(true);
    expect(res.data.records.length).toBeGreaterThanOrEqual(3);
    expect(res.data.records.some((r: any) => r.roleCode === 'ROLE_ADMIN')).toBe(true);
  });

  it('returns the nested role tree from list_tree', async () => {
    const res = await call({url: 'api/v3/auth/role/list_tree', method: 'post', data: {}});
    expect(res.ok).toBe(true);
    expect(Array.isArray(res.data)).toBe(true);
    expect(res.data.length).toBeGreaterThan(0);
    expect(res.data.some((r: any) => r.roleCode === 'ROLE_ADMIN')).toBe(true);
  });

  it('falls back gracefully for truly unregistered endpoints', async () => {
    // No handler registers this fictional endpoint — fallback must shape a
    // safe empty page instead of surfacing an error.
    const res = await call({
      url: 'api/v3/data/fictional_metric/list',
      method: 'post',
      data: {page: {current: 1, size: 12}}
    });
    expect(res.ok).toBe(true);
    expect(Array.isArray(res.data.records)).toBe(true);
  });

  it('returns the dashboard topology with layered nodes', async () => {
    const res = await call({url: 'api/v3/manager/dashboard/topology', method: 'get'});
    expect(res.data.nodes.length).toBeGreaterThan(0);
    expect(res.data.stats.driverCount).toBeGreaterThan(0);
    expect(res.data.nodes.filter((node: any) => node.type === 'driver').length).toBeLessThanOrEqual(10);
    expect(res.data.nodes.filter((node: any) => node.type === 'device').length).toBeLessThanOrEqual(20);
  });

  it('returns complete current dashboard time payloads', async () => {
    const series = await call({
      url: 'api/v3/data/dashboard/stats/timeseries',
      method: 'get',
      params: {range_key: '24h', granularity: 'hour'},
    });
    expect(series.data).toHaveLength(24);
    expect(series.data.every((row: any) => typeof row.bucket === 'string' && row.bucket.length > 0)).toBe(true);
    expect(new Set(series.data.map((row: any) => row.bucket)).size).toBe(24);

    const stream = await call({url: 'api/v3/data/dashboard/stream', method: 'get', params: {size: 1}});
    const latest = new Date(String(stream.data[0].createTime).replace(' ', 'T')).getTime();
    expect(Date.now() - latest).toBeLessThan(60_000);
  });

  it('serves rich localized Agentic Assistant conversations', async () => {
    const sessions = await call({
      url: 'api/v3/agentic/session/list',
      method: 'post',
      data: {page: {current: 1, size: 20}},
    });
    expect(sessions.data.total).toBeGreaterThanOrEqual(8);
    expect(new Set(sessions.data.records.map((session: any) => session.sessionExt.icon)).size).toBeGreaterThanOrEqual(8);
    expect(sessions.data.records.every((session: any) => !/[\u3400-\u9fff]/u.test(session.title))).toBe(true);

    const defaultSession = sessions.data.records.find((session: any) => session.sessionExt.category === 'health');
    const defaultMessages = await call({
      url: 'api/v3/agentic/message/list',
      method: 'get',
      params: {conversation_id: defaultSession.conversationId},
    });
    const finalDefaultReply = defaultMessages.data.at(-1);
    expect(defaultMessages.data.length).toBeGreaterThanOrEqual(12);
    expect(finalDefaultReply.role).toBe('assistant');
    expect(finalDefaultReply.contentExt.tools.length).toBeGreaterThanOrEqual(4);
    expect(finalDefaultReply.contentExt.charts.length).toBeGreaterThanOrEqual(3);
    expect(finalDefaultReply.contentExt.contexts.length).toBeGreaterThanOrEqual(2);

    const defaultPending = await call({
      url: 'api/v3/agentic/action/pending',
      method: 'get',
      params: {conversation_id: defaultSession.conversationId},
    });
    expect(defaultPending.data).toHaveLength(1);
    expect(defaultPending.data[0].actionType).toBe('WORK_ORDER_CREATE');

    const alarmSession = sessions.data.records.find((session: any) => session.sessionExt.category === 'alarm');
    const messages = await call({
      url: 'api/v3/agentic/message/list',
      method: 'get',
      params: {conversation_id: alarmSession.conversationId},
    });
    const richReply = messages.data.find((message: any) => message.role === 'assistant');
    expect(messages.data.length).toBeGreaterThanOrEqual(4);
    expect(richReply.content).not.toMatch(/[\u3400-\u9fff]/u);
    expect(richReply.contentExt.tools.length).toBeGreaterThanOrEqual(3);
    expect(richReply.contentExt.traces.length).toBeGreaterThanOrEqual(3);
    expect(richReply.contentExt.charts.length).toBeGreaterThanOrEqual(2);
    expect(richReply.contentExt.tokens.context).toBeGreaterThan(0);

    const commandSession = sessions.data.records.find((session: any) => session.sessionExt.category === 'command');
    const pending = await call({
      url: 'api/v3/agentic/action/pending',
      method: 'get',
      params: {conversation_id: commandSession.conversationId},
    });
    expect(pending.data).toHaveLength(1);
    expect(pending.data[0].title).not.toMatch(/[\u3400-\u9fff]/u);

    i18n.global.locale.value = 'zh';
    const localized = await call({
      url: 'api/v3/agentic/session/list',
      method: 'post',
      data: {page: {current: 1, size: 20}},
    });
    expect(localized.data.records.some((session: any) => /[\u3400-\u9fff]/u.test(session.title))).toBe(true);
  });

  it('populates every Alarm Overview diagnostic endpoint', async () => {
    const [page, trend, top, activity, types, storm, flapping, correlation, peer, mtta, changes, protocols, coverage] =
      await Promise.all([
        call({
          url: 'api/v3/data/dashboard/alert/page',
          method: 'post',
          data: {source: 'device', confirmFlag: 0, current: 1, size: 3},
        }),
        call({url: 'api/v3/data/dashboard/alert/trend', method: 'get', params: {days: 7}}),
        call({url: 'api/v3/data/dashboard/alert/top_sources', method: 'get', params: {limit: 10}}),
        call({url: 'api/v3/data/dashboard/alert/activity', method: 'get', params: {days: 7}}),
        call({url: 'api/v3/data/dashboard/alert/type_distribution', method: 'get', params: {days: 30}}),
        call({
          url: 'api/v3/data/dashboard/alert/storm_sources',
          method: 'get',
          params: {hours: 24, min_count: 100, limit: 10},
        }),
        call({url: 'api/v3/data/dashboard/alert/flapping', method: 'get', params: {min_count: 5, limit: 20}}),
        call({url: 'api/v3/data/dashboard/alert/correlation', method: 'get', params: {limit: 15}}),
        call({url: 'api/v3/data/dashboard/alert/peer_deviation', method: 'get', params: {days: 7}}),
        call({url: 'api/v3/data/dashboard/alert/mtta', method: 'get', params: {days: 7}}),
        call({url: 'api/v3/data/dashboard/alert/change_impact', method: 'get', params: {limit: 30}}),
        call({url: 'api/v3/data/dashboard/protocol/health', method: 'get'}),
        call({url: 'api/v3/data/dashboard/coverage/gap', method: 'get', params: {limit: 100}}),
      ]);

    expect(page.data.total).toBe(6);
    expect(page.data.records).toHaveLength(3);
    expect(page.data.records.every((row: any) => row.source === 'device' && row.confirmFlag === 'UNCONFIRMED')).toBe(true);
    expect(trend.data).toHaveLength(7);
    expect(top.data.length).toBeGreaterThanOrEqual(8);
    expect(activity.data).toHaveLength(7 * 24);
    expect(types.data.length).toBeGreaterThanOrEqual(5);
    expect(storm.data.every((row: any) => row.count >= 100)).toBe(true);
    expect(flapping.data.length).toBeGreaterThanOrEqual(10);
    expect(correlation.data.length).toBeGreaterThanOrEqual(10);
    expect(peer.data.length).toBeGreaterThanOrEqual(10);
    expect(mtta.data).toHaveLength(7);
    expect(changes.data.length).toBeGreaterThanOrEqual(10);
    expect(protocols.data.length).toBeGreaterThanOrEqual(10);
    expect(coverage.data.missingPoints).toBe(coverage.data.items.length);
    expect(coverage.data.items.length).toBeGreaterThanOrEqual(8);
  });

  it('returns English display data without Chinese text', async () => {
    const [profiles, points, stream, alerts, topology] = await Promise.all([
      call({url: 'api/v3/manager/profile/list_by_ids', method: 'post', data: ['2001']}),
      call({url: 'api/v3/manager/point/list_by_ids', method: 'post', data: ['5001']}),
      call({url: 'api/v3/data/dashboard/stream', method: 'get', params: {size: 1}}),
      call({url: 'api/v3/data/dashboard/alert/latest', method: 'get'}),
      call({url: 'api/v3/manager/dashboard/topology', method: 'get'}),
    ]);
    const displayText = [
      profiles.data['2001'].profileName,
      points.data['5001'].pointName,
      stream.data[0].driverName,
      stream.data[0].deviceName,
      stream.data[0].pointName,
      ...alerts.data.map((row: any) => row.message),
      ...topology.data.nodes.map((node: any) => node.name),
    ].join(' ');

    expect(displayText).not.toMatch(/[\u3400-\u9fff]/u);
    expect(profiles.data['2001'].profileName).toBe('Temperature & Humidity Sensor');
    expect(alerts.data[0].message).toContain('offline');
  });

  it('returns Chinese display names and messages after switching locale', async () => {
    i18n.global.locale.value = 'zh';
    const [drivers, profiles, stream, alerts, topology] = await Promise.all([
      call({url: 'api/v3/manager/driver/list_by_ids', method: 'post', data: ['1001']}),
      call({url: 'api/v3/manager/profile/list_by_ids', method: 'post', data: ['2001']}),
      call({url: 'api/v3/data/dashboard/stream', method: 'get', params: {size: 1}}),
      call({url: 'api/v3/data/dashboard/alert/latest', method: 'get'}),
      call({url: 'api/v3/manager/dashboard/topology', method: 'get'}),
    ]);

    expect(drivers.data['1001'].driverName).toBe('Modbus-TCP 驱动');
    expect(profiles.data['2001'].profileName).toBe('温湿度传感器');
    expect(stream.data[0].deviceName).toMatch(/[\u3400-\u9fff]/u);
    expect(alerts.data[0].message).toBe('设备离线超过 5 分钟');
    expect(topology.data.nodes.some((node: any) => /[\u3400-\u9fff]/u.test(node.name))).toBe(true);
  });
});
