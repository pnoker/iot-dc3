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

import {chromium} from 'playwright';
import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {firstRecord, idOf, login, shortText, waitPage} from './browser-sweep/support.mjs';

const BASE = process.env.E2E_BASE_URL || 'http://localhost:8080';
const CHROME = process.env.E2E_CHROME_PATH || '';
const HEADLESS = process.env.E2E_HEADLESS !== 'false';
const VIEWPORT_WIDTH = Number(process.env.E2E_SCREENSHOT_WIDTH || 1920);
const VIEWPORT_HEIGHT = Number(process.env.E2E_SCREENSHOT_HEIGHT || 1080);
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const OUT_DIR = path.resolve(__dirname, '../../artifacts/page-screenshots');

function isBusinessApi(url) {
  return url.includes('/api/v3/');
}

function createScreenshotWatch(page) {
  const state = {
    pageErrors: [],
    consoleErrors: [],
    consoleWarnings: [],
    badResponses: [],
  };

  page.on('pageerror', (err) => state.pageErrors.push(err.message));
  page.on('console', (msg) => {
    const type = msg.type();
    const text = shortText(msg.text(), 500);
    if (type === 'error') state.consoleErrors.push(text);
    if (type === 'warning') state.consoleWarnings.push(text);
  });
  page.on('response', async (res) => {
    if (!isBusinessApi(res.url()) || res.status() < 400) return;
    const body = await res.text().catch(() => '');
    state.badResponses.push({status: res.status(), url: res.url(), body: shortText(body, 500)});
  });

  return state;
}

function markWatch(watch) {
  return {
    pageErrors: watch.pageErrors.length,
    consoleErrors: watch.consoleErrors.length,
    consoleWarnings: watch.consoleWarnings.length,
    badResponses: watch.badResponses.length,
  };
}

function diffWatch(watch, mark) {
  return {
    pageErrors: watch.pageErrors.slice(mark.pageErrors),
    consoleErrors: watch.consoleErrors.slice(mark.consoleErrors),
    consoleWarnings: watch.consoleWarnings.slice(mark.consoleWarnings),
    badResponses: watch.badResponses.slice(mark.badResponses),
  };
}

function assertClean(step, watch, mark) {
  const diff = diffWatch(watch, mark);
  if (diff.pageErrors.length || diff.consoleErrors.length || diff.consoleWarnings.length || diff.badResponses.length) {
    throw new Error(`${step} failed: ${JSON.stringify(diff, null, 2)}`);
  }
}

async function settle(page, extraDelay = 900) {
  await waitPage(page);
  await page.waitForTimeout(extraDelay);
}

function cacheBustRoute(route) {
  const token = `${Date.now()}-${Math.random().toString(36).slice(2)}`;
  return `${route}${route.includes('?') ? '&' : '?'}__shot=${token}`;
}

function fallbackRouteFor(route) {
  return route.split('?')[0] === '/settings/about' ? '/home' : '/settings/about';
}

async function gotoFallback(page, route) {
  await page.goto(`${BASE}/#${cacheBustRoute(fallbackRouteFor(route))}`, {waitUntil: 'domcontentloaded'});
  await settle(page, 250);
}

function assertPattern(pattern, text, step, target) {
  if (!pattern.test(text)) {
    throw new Error(`${step} ${target} mismatch: expected ${pattern}, got "${text}"`);
  }
}

async function expectActiveTab(page, pattern, step) {
  const tab = page.locator('.el-tabs__item.is-active:visible').first();
  await tab.waitFor({state: 'visible', timeout: 10000});
  assertPattern(pattern, (await tab.innerText()).trim(), step, 'active tab');
}

async function expectEditDivider(page, pattern, step) {
  const tab = page.locator('.el-tabs__item.is-active:visible').first();
  await tab.waitFor({state: 'visible', timeout: 10000});
  assertPattern(pattern, (await tab.innerText()).trim(), step, 'edit tab');
}

async function discoverIds(page) {
  const [driver, profile, device, point, api, resource, menu, user, role] = await Promise.all([
    firstRecord(page, '/api/v3/manager/driver/list'),
    firstRecord(page, '/api/v3/manager/profile/list'),
    firstRecord(page, '/api/v3/manager/device/list'),
    firstRecord(page, '/api/v3/manager/point/list'),
    firstRecord(page, '/api/v3/auth/api/list'),
    firstRecord(page, '/api/v3/auth/resource/list'),
    firstRecord(page, '/api/v3/auth/menu/list'),
    firstRecord(page, '/api/v3/auth/user_profile/list'),
    firstRecord(page, '/api/v3/auth/role/list'),
  ]);

  return {
    driverId: idOf(driver),
    deviceId: idOf(device),
    profileId: idOf(profile),
    pointId: idOf(point),
    pointProfileId: point?.profileId ? String(point.profileId) : idOf(profile),
    apiId: idOf(api),
    resourceId: idOf(resource),
    menuId: idOf(menu),
    userId: idOf(user),
    roleId: idOf(role),
  };
}

async function clickByRoleName(page, name, root = page) {
  const locator = root.getByRole('button', {name}).first();
  if (!(await locator.count())) return false;
  if (!(await locator.isVisible().catch(() => false))) return false;
  await locator.click();
  return true;
}

async function openAddDialog(page) {
  const clicked =
    (await clickByRoleName(page, /^(Add|新增)$/)) ||
    (await page
      .locator('button.el-button:visible', {hasText: /Add|新增/})
      .first()
      .click()
      .then(() => true)
      .catch(() => false));
  if (!clicked) throw new Error(`Cannot find Add button on ${page.url()}`);

  const dialog = page.locator('.el-dialog:visible').last();
  await dialog.waitFor({state: 'visible', timeout: 10000});
  await settle(page, 300);
  return dialog;
}

async function fillFirstByPlaceholder(root, placeholder, value) {
  const locator = root.getByPlaceholder(placeholder).first();
  if (!(await locator.count())) return false;
  await locator.fill(value);
  return true;
}

async function fillTextarea(root, value) {
  const textarea = root.locator('textarea:visible').last();
  if (!(await textarea.count())) return false;
  await textarea.fill(value);
  return true;
}

async function openFirstSelectOption(root, page, selector = '.el-select:visible') {
  const select = root.locator(selector).first();
  if (!(await select.count())) return false;
  await select.click();
  await page.waitForTimeout(250);
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item:not(.is-disabled)').first();
  if (!(await option.count())) {
    await page.keyboard.press('Escape').catch(() => {
    });
    return false;
  }
  await option.click();
  await page.waitForTimeout(250);
  return true;
}

async function clickEventTab(page, label) {
  const tab = page.locator('.event-overview__tabs .el-tabs__item:visible', {hasText: label}).first();
  if (!(await tab.count())) throw new Error(`Cannot find event tab: ${label}`);
  await tab.click();
  await settle(page, 1200);
}

async function expandAppScrollForFullCapture(page) {
  await page
    .locator('.body-main .el-scrollbar__wrap')
    .evaluate((el) => {
      el.scrollTop = 0;
    })
    .catch(() => {
    });

  return page.addStyleTag({
    content: `
      html,
      body,
      #app,
      .container {
        height: auto !important;
        min-height: 100vh !important;
        overflow: visible !important;
      }

      .container .body {
        position: static !important;
        height: auto !important;
        min-height: calc(100vh - 60px) !important;
        overflow: visible !important;
      }

      .container .body-main,
      .container .body-main > .el-scrollbar,
      .container .body-main .el-scrollbar__wrap {
        height: auto !important;
        max-height: none !important;
        overflow: visible !important;
      }

      .container .body-main .el-scrollbar__bar {
        display: none !important;
      }
    `,
  });
}

async function captureScreenshot(page, shot, target) {
  if (!shot.expandAppScroll) {
    await page.screenshot({path: target, fullPage: shot.fullPage !== false});
    return;
  }

  const style = await expandAppScrollForFullCapture(page);
  await page.waitForTimeout(500);
  try {
    await page.screenshot({path: target, fullPage: true});
  } finally {
    await style.evaluate((node) => node.remove()).catch(() => {
    });
  }
}

async function screenshotPage(page, watch, shot) {
  await gotoFallback(page, shot.route);
  const mark = markWatch(watch);
  await page.goto(`${BASE}/#${cacheBustRoute(shot.route)}`, {waitUntil: 'domcontentloaded'});
  await settle(page, shot.wait ?? 900);
  if (shot.prepare) {
    await shot.prepare(page);
    await settle(page, shot.waitAfterPrepare ?? 900);
  }
  if (shot.expectActiveTab) await expectActiveTab(page, shot.expectActiveTab, shot.name);
  if (shot.expectEditDivider) await expectEditDivider(page, shot.expectEditDivider, shot.name);

  const target = path.join(OUT_DIR, shot.file);
  await captureScreenshot(page, shot, target);
  assertClean(shot.name, watch, mark);
  console.log(`${shot.file} ${shot.route}`);
}

function buildShots(ids) {
  const shots = [];
  const full = {expandAppScroll: true};
  const add = (slug, name, route, options = {}) => {
    shots.push({slug, name, route, ...options});
  };

  add('home-dashboard', 'Home dashboard', '/home', {wait: 1600, ...full});

  add('driver-list', 'Driver list', '/driver', full);
  add('driver-detail-info', 'Driver detail info tab', `/driver/detail?id=${ids.driverId}&active=detail`, {
    expectActiveTab: /Driver Info|驱动信息/,
    ...full,
  });
  add('driver-detail-devices', 'Driver detail devices tab', `/driver/detail?id=${ids.driverId}&active=device`, {
    wait: 1200,
    expectActiveTab: /Related Devices|关联设备/,
    ...full,
  });

  add('profile-list', 'Profile list', '/profile', full);
  add('profile-detail-info', 'Profile detail info tab', `/profile/detail?id=${ids.profileId}&active=detail`, {
    expectActiveTab: /Profile Info|模板信息/,
    ...full,
  });
  add('profile-detail-points', 'Profile detail points tab', `/profile/detail?id=${ids.profileId}&active=point`, {
    wait: 1200,
    expectActiveTab: /Related Points|关联位号/,
    ...full,
  });
  add('profile-detail-devices', 'Profile detail devices tab', `/profile/detail?id=${ids.profileId}&active=device`, {
    wait: 1200,
    expectActiveTab: /Related Devices|关联设备/,
    ...full,
  });
  add(
    'profile-edit-step-1-profile-config',
    'Profile edit step 1 profile config',
    `/profile/edit?id=${ids.profileId}&active=profileConfig`,
    {
      expectEditDivider: /Profile Info|模板信息配置/,
      ...full,
    }
  );
  add(
    'profile-edit-step-2-point-config',
    'Profile edit step 2 point config',
    `/profile/edit?id=${ids.profileId}&active=pointConfig`,
    {
      wait: 1200,
      expectEditDivider: /Profile Points|模板位号配置/,
      ...full,
    }
  );
  add(
    'profile-edit-step-3-command-config',
    'Profile edit step 3 command config',
    `/profile/edit?id=${ids.profileId}&active=commandConfig`,
    {
      expectEditDivider: /Profile Commands|模板指令配置/,
      ...full,
    }
  );

  add('device-list', 'Device list', '/device', full);
  add('device-detail-info', 'Device detail info tab', `/device/detail?id=${ids.deviceId}&active=detail`, {
    expectActiveTab: /Device Info|设备信息/,
    ...full,
  });
  add('device-detail-profiles', 'Device detail profiles tab', `/device/detail?id=${ids.deviceId}&active=profile`, {
    wait: 1200,
    expectActiveTab: /Related Profiles|关联模板/,
    ...full,
  });
  add('device-detail-points', 'Device detail points tab', `/device/detail?id=${ids.deviceId}&active=point`, {
    wait: 1200,
    expectActiveTab: /Related Points|关联位号/,
    ...full,
  });
  add(
    'device-detail-point-values',
    'Device detail point values tab',
    `/device/detail?id=${ids.deviceId}&active=pointValue`,
    {
      wait: 1400,
      expectActiveTab: /Device Data|设备数据/,
      ...full,
    }
  );
  add(
    'device-edit-step-1-device-config',
    'Device edit step 1 device config',
    `/device/edit?id=${ids.deviceId}&active=deviceConfig`,
    {
      expectEditDivider: /Device Info|设备信息配置/,
      ...full,
    }
  );
  add(
    'device-edit-step-2-driver-config',
    'Device edit step 2 driver config',
    `/device/edit?id=${ids.deviceId}&active=driverConfig`,
    {
      wait: 1200,
      expectEditDivider: /Driver Attributes|驱动属性配置/,
      ...full,
    }
  );
  add(
    'device-edit-step-3-point-config',
    'Device edit step 3 point config',
    `/device/edit?id=${ids.deviceId}&active=pointConfig`,
    {
      wait: 1400,
      expectEditDivider: /Point Attributes|位号属性配置/,
      ...full,
    }
  );
  add(
    'device-edit-step-4-command-config',
    'Device edit step 4 command config',
    `/device/edit?id=${ids.deviceId}&active=commandConfig`,
    {
      expectEditDivider: /Related Commands|Device Commands|设备指令能力/,
      ...full,
    }
  );

  add('point-detail-info', 'Point detail info tab', `/point/detail?id=${ids.pointId}&active=detail`, {
    expectActiveTab: /Point Info|位号信息/,
    ...full,
  });
  add('point-detail-devices', 'Point detail devices tab', `/point/detail?id=${ids.pointId}&active=device`, {
    wait: 1200,
    expectActiveTab: /Related Devices|关联设备/,
    ...full,
  });
  add('point-value-list', 'Point value list', '/point_value', full);
  add('settings-user-list', 'Settings user list', '/settings/user', full);
  add('settings-user-add-form', 'Settings user add form', '/settings/user', {
    prepare: async (page) => {
      const dialog = await openAddDialog(page);
      await fillFirstByPlaceholder(dialog, 'Enter user name', 'zhang.wei.ops');
      await fillFirstByPlaceholder(dialog, 'Enter nickname', 'Zhang Wei - Operations Lead');
      await fillFirstByPlaceholder(dialog, 'Enter phone number', '13816881234');
      await fillFirstByPlaceholder(dialog, 'Enter email', 'zhang.wei.ops@example.com');
    },
  });
  add(
    'settings-user-detail-info',
    'Settings user detail info tab',
    `/settings/user/detail?id=${ids.userId}&active=detail`,
    {
      expectActiveTab: /User Detail|用户详情/,
      ...full,
    }
  );
  add(
    'settings-user-detail-roles',
    'Settings user detail roles tab',
    `/settings/user/detail?id=${ids.userId}&active=role`,
    {
      wait: 1200,
      expectActiveTab: /Assigned Roles|已分配角色/,
      ...full,
    }
  );
  add(
    'settings-user-detail-resources',
    'Settings user detail resources tab',
    `/settings/user/detail?id=${ids.userId}&active=resource`,
    {wait: 1200, expectActiveTab: /Accessible Resources|可访问资源/, ...full}
  );

  add('settings-role-list', 'Settings role list', '/settings/role', full);
  add('settings-role-add-form', 'Settings role add form', '/settings/role', {
    prepare: async (page) => {
      const dialog = await openAddDialog(page);
      await fillFirstByPlaceholder(dialog, 'Enter role name', 'Regional Operations Viewer');
      await fillFirstByPlaceholder(dialog, 'Enter role code', 'OPS_REGIONAL_VIEWER');
      await fillTextarea(dialog, 'Read-only access for East China operation centers and vendor audit reviews.');
    },
  });
  add(
    'settings-role-detail-info',
    'Settings role detail info tab',
    `/settings/role/detail?id=${ids.roleId}&active=detail`,
    {
      expectActiveTab: /Role Detail|角色详情/,
      ...full,
    }
  );
  add(
    'settings-role-detail-users',
    'Settings role detail users tab',
    `/settings/role/detail?id=${ids.roleId}&active=user`,
    {
      wait: 1200,
      expectActiveTab: /Assigned Users|关联用户/,
      ...full,
    }
  );
  add(
    'settings-role-detail-resources',
    'Settings role detail resources tab',
    `/settings/role/detail?id=${ids.roleId}&active=resource`,
    {wait: 1200, expectActiveTab: /Assigned Resources|已分配资源/, ...full}
  );

  add('settings-resource-list', 'Settings resource list', '/settings/resource', full);
  add(
    'settings-resource-detail-info',
    'Settings resource detail info tab',
    `/settings/resource/detail?id=${ids.resourceId}&active=detail`,
    {
      expectActiveTab: /Resource Detail|资源详情/,
      ...full,
    }
  );
  add(
    'settings-resource-detail-roles',
    'Settings resource detail roles tab',
    `/settings/resource/detail?id=${ids.resourceId}&active=role`,
    {wait: 1200, expectActiveTab: /Assigned Roles|关联角色/, ...full}
  );
  add(
    'settings-resource-detail-children',
    'Settings resource detail children tab',
    `/settings/resource/detail?id=${ids.resourceId}&active=children`,
    {wait: 1200, expectActiveTab: /Child Resources|子资源/, ...full}
  );

  add('settings-api-list', 'Settings API list', '/settings/api', full);
  add('settings-api-detail', 'Settings API detail', `/settings/api/detail?id=${ids.apiId}`, full);
  add('settings-menu-list', 'Settings menu list', '/settings/menu', full);
  add('settings-menu-detail', 'Settings menu detail', `/settings/menu/detail?id=${ids.menuId}`, full);

  add('settings-group-list', 'Settings group list', '/settings/group', full);
  add('settings-group-add-form', 'Settings group add form', '/settings/group', {
    prepare: async (page) => {
      const dialog = await openAddDialog(page);
      await fillFirstByPlaceholder(dialog, 'Enter group name', 'Beijing Pilot Plant');
      await fillFirstByPlaceholder(dialog, 'Enter group code', 'demo.group.site.beijing.pilot');
      await fillTextarea(dialog, 'Commissioning group for the Beijing pilot plant chilled-water loop.');
    },
  });
  add('settings-label-list', 'Settings label list', '/settings/label', full);
  add('settings-label-add-form', 'Settings label add form', '/settings/label', {
    prepare: async (page) => {
      const dialog = await openAddDialog(page);
      await fillFirstByPlaceholder(dialog, 'Enter label name', 'Compressor Watch');
      await fillFirstByPlaceholder(dialog, 'Enter label code', 'demo.label.compressor.watch');
      await fillTextarea(dialog, 'Used for compressor pressure, vibration, and discharge temperature watch points.');
    },
  });

  add('settings-model-config-list', 'Settings model config list', '/settings/model/config', full);
  add('settings-model-config-add-form', 'Settings model config add form', '/settings/model/config', {
    prepare: async (page) => {
      const dialog = await openAddDialog(page);
      await fillFirstByPlaceholder(dialog, 'gpt-4o-mini', 'gpt-4.1-mini');
      await fillFirstByPlaceholder(dialog, 'GPT-4o Mini', 'GPT-4.1 Mini - Ops');
      await openFirstSelectOption(dialog, page);
      await fillTextarea(dialog, 'Default lightweight model for device triage and event summarization demos.');
    },
  });
  add('settings-model-provider-list', 'Settings model provider list', '/settings/model/provider', full);
  add('settings-model-provider-add-form', 'Settings model provider add form', '/settings/model/provider', {
    prepare: async (page) => {
      const dialog = await openAddDialog(page);
      await fillFirstByPlaceholder(dialog, 'My Provider', 'OpenAI East China Relay');
      await fillFirstByPlaceholder(dialog, 'https://api.openai.com', 'https://api.openai.com/v1');
      await fillFirstByPlaceholder(dialog, 'sk-...', 'sk-demo-redacted-202605');
      await fillTextarea(dialog, 'Demo relay endpoint used by the East China operation center.');
    },
  });

  add('settings-event-overview-situation', 'Settings alarm overview situation', '/settings/alarm/overview', {
    wait: 1600,
    ...full,
  });
  add('settings-event-overview-noise', 'Settings alarm overview noise', '/settings/alarm/overview', {
    wait: 1600,
    prepare: (page) => clickEventTab(page, 'Noise'),
    ...full,
  });
  add('settings-event-overview-availability', 'Settings alarm overview availability', '/settings/alarm/overview', {
    wait: 1600,
    prepare: (page) => clickEventTab(page, 'Availability'),
    ...full,
  });
  add('settings-event-overview-sla', 'Settings alarm overview SLA', '/settings/alarm/overview', {
    wait: 1600,
    prepare: (page) => clickEventTab(page, 'SLA'),
    ...full,
  });
  add('settings-device-event-list', 'Settings device alarm list', '/settings/alarm/device', full);
  add('settings-driver-event-list', 'Settings driver alarm list', '/settings/alarm/driver', full);
  add('settings-about', 'Settings about', '/settings/about', full);

  add('ai-assistant-panel', 'AI assistant panel', '/home', {
    wait: 1600,
    prepare: async (page) => {
      const launcher = page.locator('.agentic-launcher:visible').first();
      if (!(await launcher.count())) throw new Error('Cannot find AI assistant launcher');
      await launcher.click();
      await page.locator('.agentic-panel:visible').waitFor({state: 'visible', timeout: 10000});
    },
  });

  return shots.map((shot, index) => ({
    ...shot,
    file: `${String(index + 1).padStart(3, '0')}-${shot.slug}.png`,
  }));
}

const launchOptions = {headless: HEADLESS};
if (CHROME) launchOptions.executablePath = CHROME;

await fs.rm(OUT_DIR, {force: true, recursive: true});
await fs.mkdir(OUT_DIR, {recursive: true});

const browser = await chromium.launch(launchOptions);
const context = await browser.newContext({
  viewport: {width: VIEWPORT_WIDTH, height: VIEWPORT_HEIGHT},
  deviceScaleFactor: 1,
});
const page = await context.newPage();
const watch = createScreenshotWatch(page);

try {
  await login(page, BASE);
  const ids = await discoverIds(page);
  for (const required of [
    'driverId',
    'deviceId',
    'profileId',
    'pointId',
    'pointProfileId',
    'apiId',
    'resourceId',
    'menuId',
    'userId',
    'roleId',
  ]) {
    if (!ids[required]) throw new Error(`Cannot discover route id: ${required}`);
  }

  const shots = buildShots(ids);
  for (const shot of shots) {
    await screenshotPage(page, watch, shot);
  }

  console.log(`Saved ${shots.length} screenshots to ${OUT_DIR}`);
} finally {
  await context.close();
  await browser.close();
}
