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

  // The response interceptor returns the direct resource payload.
  const call = (config: AxiosRequestConfig) => request(config) as Promise<any>;

  it('does not pre-seed auth so the demo lands on /login', () => {
    expect(localStorage.getItem('X-Auth-Tenant')).toBeNull();
    expect(localStorage.getItem('X-Auth-Token')).toBeNull();
  });

  it('returns the full menu tree from list_tree', async () => {
    const res = await call({url: 'api/v3/auth/menu/list_tree', method: 'post', data: {}});
        expect(Array.isArray(res)).toBe(true);
    expect(res.some((n: any) => n.menuCode === 'driver')).toBe(true);
    expect(res.some((n: any) => n.menuCode === 'settings')).toBe(true);
  });

  it('paginates core lists into a PageResult', async () => {
    const res = await call({url: 'api/v3/manager/driver/list', method: 'post', data: {offset: 0, limit: 12}});
    expect(res.total).toBeGreaterThan(0);
    expect(Array.isArray(res.items)).toBe(true);
    expect(res.items.length).toBeGreaterThan(0);
  });

  it('resolves get_by_id by id', async () => {
    const res = await call({url: 'api/v3/manager/driver/get_by_id', method: 'get', params: {id: '1001'}});
    expect(res.id).toBe('1001');
  });

  it('returns a driver dictionary with label/value', async () => {
    const res = await call({url: 'api/v3/manager/dictionary/list_driver', method: 'post', data: {offset: 0, limit: 1}});
    expect(res.items[0]).toHaveProperty('label');
    expect(res.items[0]).toHaveProperty('value');
    expect(res.items).toHaveLength(1);
    expect(res.limit).toBe(1);
  });

  it('returns tenant dictionary leaves with stable fields', async () => {
    const res = await call({url: 'api/v3/auth/dictionary/list_tenant', method: 'get'});
    expect(res).toEqual([
      {
        label: 'Default Tenant',
        value: 'default',
        disabled: false,
        expand: false,
        children: [],
      },
    ]);
  });

  it('mocks the token endpoints so login works end-to-end', async () => {
    const login = {tenant: 'default', name: 'dc3', password: 'dc3dc3dc3'};
    const salt = await call({url: 'api/v3/auth/token/salt', method: 'post', data: login});
    expect(salt).toBe('mock-salt');
    const token = await call({url: 'api/v3/auth/token/generate', method: 'post', data: login});
    expect(token).toBe('ok');

    await expect(
      call({url: 'api/v3/auth/token/generate', method: 'post', data: {...login, password: 'incorrect'}}),
    ).rejects.toMatchObject({status: 401, code: 'R4010'});
  });

  it('returns the seeded role list with real data', async () => {
    const res = await call({url: 'api/v3/auth/role/list', method: 'post', data: {offset: 0, limit: 12}});
        expect(res.items.length).toBeGreaterThanOrEqual(3);
    expect(res.items.some((r: any) => r.roleCode === 'ROLE_ADMIN')).toBe(true);
  });

  it('returns the nested role tree from list_tree', async () => {
    const res = await call({url: 'api/v3/auth/role/list_tree', method: 'post', data: {}});
        expect(Array.isArray(res)).toBe(true);
    expect(res.length).toBeGreaterThan(0);
    expect(res.some((r: any) => r.roleCode === 'ROLE_ADMIN')).toBe(true);
  });

  it('falls back gracefully for truly unregistered endpoints', async () => {
    // No handler registers this fictional endpoint — fallback must shape a
    // safe empty page instead of surfacing an error.
    const res = await call({
      url: 'api/v3/data/fictional_metric/list',
      method: 'post',
      data: {offset: 0, limit: 12}
    });
        expect(Array.isArray(res.items)).toBe(true);
  });

  it('returns the dashboard topology with layered nodes', async () => {
    const res = await call({url: 'api/v3/manager/dashboard/topology', method: 'get'});
    expect(res.nodes.length).toBeGreaterThan(0);
    expect(res.stats.driverCount).toBeGreaterThan(0);
    expect(res.nodes.filter((node: any) => node.type === 'driver').length).toBeLessThanOrEqual(10);
    expect(res.nodes.filter((node: any) => node.type === 'device').length).toBeLessThanOrEqual(20);
  });

  it('returns complete current dashboard time payloads', async () => {
    const series = await call({
      url: 'api/v3/data/dashboard/stats/timeseries',
      method: 'get',
      params: {range_key: '24h', granularity: 'hour'},
    });
    expect(series).toHaveLength(24);
    expect(series.every((row: any) => typeof row.bucket === 'string' && row.bucket.length > 0)).toBe(true);
    expect(new Set(series.map((row: any) => row.bucket)).size).toBe(24);

    const stream = await call({url: 'api/v3/data/dashboard/stream', method: 'get', params: {limit: 1}});
    const latest = new Date(String(stream[0].createTime).replace(' ', 'T')).getTime();
    expect(Date.now() - latest).toBeLessThan(60_000);
  });

  it('serves rich localized Agentic Assistant conversations', async () => {
    const sessions = await call({
      url: 'api/v3/agentic/session/list',
      method: 'post',
      data: {offset: 0, limit: 20},
    });
    expect(sessions.total).toBeGreaterThanOrEqual(8);
    expect(new Set(sessions.items.map((session: any) => session.sessionExt.icon)).size).toBeGreaterThanOrEqual(8);
    expect(sessions.items.every((session: any) => !/[\u3400-\u9fff]/u.test(session.title))).toBe(true);

    const defaultSession = sessions.items.find((session: any) => session.sessionExt.category === 'health');
    const defaultMessages = await call({
      url: 'api/v3/agentic/message/list',
      method: 'get',
      params: {conversation_id: defaultSession.conversationId},
    });
    const finalDefaultReply = defaultMessages.at(-1);
    expect(defaultMessages.length).toBeGreaterThanOrEqual(12);
    expect(finalDefaultReply.role).toBe('assistant');
    expect(finalDefaultReply.contentExt.tools.length).toBeGreaterThanOrEqual(4);
    expect(finalDefaultReply.contentExt.charts.length).toBeGreaterThanOrEqual(3);
    expect(finalDefaultReply.contentExt.contexts.length).toBeGreaterThanOrEqual(2);

    const defaultPending = await call({
      url: 'api/v3/agentic/action/pending',
      method: 'get',
      params: {conversation_id: defaultSession.conversationId},
    });
    expect(defaultPending.items).toHaveLength(1);
    expect(defaultPending.items[0].actionType).toBe('WORK_ORDER_CREATE');

    const alarmSession = sessions.items.find((session: any) => session.sessionExt.category === 'alarm');
    const messages = await call({
      url: 'api/v3/agentic/message/list',
      method: 'get',
      params: {conversation_id: alarmSession.conversationId},
    });
    const richReply = messages.find((message: any) => message.role === 'assistant');
    expect(messages.length).toBeGreaterThanOrEqual(4);
    expect(richReply.content).not.toMatch(/[\u3400-\u9fff]/u);
    expect(richReply.contentExt.tools.length).toBeGreaterThanOrEqual(3);
    expect(richReply.contentExt.traces.length).toBeGreaterThanOrEqual(3);
    expect(richReply.contentExt.charts.length).toBeGreaterThanOrEqual(2);
    expect(richReply.contentExt.tokens.context).toBeGreaterThan(0);

    const commandSession = sessions.items.find((session: any) => session.sessionExt.category === 'command');
    const pending = await call({
      url: 'api/v3/agentic/action/pending',
      method: 'get',
      params: {conversation_id: commandSession.conversationId},
    });
    expect(pending.items).toHaveLength(1);
    expect(pending.items[0].title).not.toMatch(/[\u3400-\u9fff]/u);

    i18n.global.locale.value = 'zh';
    const localized = await call({
      url: 'api/v3/agentic/session/list',
      method: 'post',
      data: {offset: 0, limit: 20},
    });
    expect(localized.items.some((session: any) => /[\u3400-\u9fff]/u.test(session.title))).toBe(true);
  });

  it('populates every Alarm Overview diagnostic endpoint', async () => {
    const [page, trend, top, activity, types, storm, flapping, correlation, peer, mtta, changes, protocols, coverage] =
      await Promise.all([
        call({
          url: 'api/v3/data/dashboard/alert/page',
          method: 'post',
          data: {source: 'device', confirmFlag: 0, offset: 0, limit: 3},
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

    expect(page.total).toBe(6);
    expect(page.items).toHaveLength(3);
    expect(page.items.every((row: any) => row.source === 'device' && row.confirmFlag === 'UNCONFIRMED')).toBe(true);
    expect(trend).toHaveLength(7);
    expect(top.length).toBeGreaterThanOrEqual(8);
    expect(activity).toHaveLength(7 * 24);
    expect(types.length).toBeGreaterThanOrEqual(5);
    expect(storm.every((row: any) => row.count >= 100)).toBe(true);
    expect(flapping.length).toBeGreaterThanOrEqual(10);
    expect(correlation.length).toBeGreaterThanOrEqual(10);
    expect(peer.length).toBeGreaterThanOrEqual(10);
    expect(mtta).toHaveLength(7);
    expect(changes.length).toBeGreaterThanOrEqual(10);
    expect(protocols.length).toBeGreaterThanOrEqual(10);
    expect(coverage.missingPoints).toBe(coverage.items.length);
    expect(coverage.items.length).toBeGreaterThanOrEqual(8);
  });

  it('returns English display data without Chinese text', async () => {
    const [profiles, points, stream, alerts, topology] = await Promise.all([
      call({url: 'api/v3/manager/profile/list_by_ids', method: 'post', data: ['2001']}),
      call({url: 'api/v3/manager/point/list_by_ids', method: 'post', data: ['5001']}),
      call({url: 'api/v3/data/dashboard/stream', method: 'get', params: {limit: 1}}),
      call({url: 'api/v3/data/dashboard/alert/latest', method: 'get'}),
      call({url: 'api/v3/manager/dashboard/topology', method: 'get'}),
    ]);
    const displayText = [
      profiles['2001'].profileName,
      points['5001'].pointName,
      stream[0].driverName,
      stream[0].deviceName,
      stream[0].pointName,
      ...alerts.map((row: any) => row.message),
      ...topology.nodes.map((node: any) => node.name),
    ].join(' ');

    expect(displayText).not.toMatch(/[\u3400-\u9fff]/u);
    expect(profiles['2001'].profileName).toBe('Temperature & Humidity Sensor');
    expect(alerts[0].message).toContain('offline');
  });

  it('returns Chinese display names and messages after switching locale', async () => {
    i18n.global.locale.value = 'zh';
    const [drivers, profiles, stream, alerts, topology] = await Promise.all([
      call({url: 'api/v3/manager/driver/list_by_ids', method: 'post', data: ['1001']}),
      call({url: 'api/v3/manager/profile/list_by_ids', method: 'post', data: ['2001']}),
      call({url: 'api/v3/data/dashboard/stream', method: 'get', params: {limit: 1}}),
      call({url: 'api/v3/data/dashboard/alert/latest', method: 'get'}),
      call({url: 'api/v3/manager/dashboard/topology', method: 'get'}),
    ]);

    expect(drivers['1001'].driverName).toBe('Modbus-TCP 驱动');
    expect(profiles['2001'].profileName).toBe('温湿度传感器');
    expect(stream[0].deviceName).toMatch(/[\u3400-\u9fff]/u);
    expect(alerts[0].message).toBe('设备离线超过 5 分钟');
    expect(topology.nodes.some((node: any) => /[\u3400-\u9fff]/u.test(node.name))).toBe(true);
  });
});
