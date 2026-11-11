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

import {expect, type Page, test} from '@playwright/test';

import {expectHealthy, login, waitForAppSettled, watchPageHealth} from '../fixtures/app';

/**
 * Agent assistant e2e spec.
 *
 * The black-box contract for the assistant, in one login session:
 *   - header entry opens the panel; Escape closes it (graded exit from workbench)
 *   - blank sends are no-ops; hostile payloads render inert (no script exec)
 *   - workbench mode records its state in the URL and survives a reload
 *   - the sessions rail item menu drives rename/archive
 */

const composer = (page: Page) => page.locator('.el-textarea__inner').first();

// One assistant morph beat: the panel width, the shell's lane paddings and
// the rail collapse all ride --dc3-agentic-morph-duration (320ms). Geometry
// measured mid-sweep captures transitional values, so every measurement
// waits past the sweep before reading boxes.
const MORPH_SETTLE_MS = 400;

interface ColumnBox {
  cx: number;
  w: number;
}

interface ReadingColumnGeometry {
  laneCx: number | null;
  input: ColumnBox | null;
  stream: ColumnBox | null;
}

// The workbench's reading column (messages + composer input) centres on the
// chat lane: the shell's content box between the OVERLAY lanes (sessions
// rail left, context panel right). The shell's border box spans the whole
// workbench, so its raw centre is not the column axis.
const readingColumnGeometry = (page: Page): Promise<ReadingColumnGeometry> =>
  page.evaluate<ReadingColumnGeometry>(() => {
    const box = (sel: string): ColumnBox | null => {
      const el = document.querySelector(sel);
      if (!el) return null;
      const r = el.getBoundingClientRect();
      return {cx: Math.round(r.x + r.width / 2), w: Math.round(r.width)};
    };
    const shell = document.querySelector<HTMLElement>('.agentic-shell');
    if (!shell) return {laneCx: null, input: null, stream: null};
    const rect = shell.getBoundingClientRect();
    const style = getComputedStyle(shell);
    const padLeft = Number.parseFloat(style.paddingLeft) || 0;
    const padRight = Number.parseFloat(style.paddingRight) || 0;
    return {
      laneCx: Math.round(rect.x + padLeft + (rect.width - padLeft - padRight) / 2),
      input: box('.agentic-input-shell'),
      stream: box('.agentic-messages'),
    };
  });

// The middle column is the reference axis: input and stream both centre on
// it and share one width.
const expectColumnOnChatLane = (geometry: ReadingColumnGeometry) => {
  expect(Math.abs((geometry.input?.cx ?? -1) - (geometry.laneCx ?? -2))).toBeLessThanOrEqual(2);
  if (geometry.stream) {
    expect(geometry.input?.w).toBe(geometry.stream.w);
    expect(Math.abs(geometry.stream.cx - (geometry.laneCx ?? -2))).toBeLessThanOrEqual(2);
  }
};

test.describe('agent assistant', () => {
  test('opens from the header and closes with Escape', async ({page}) => {
    const health = watchPageHealth(page);
    await login(page);
    await waitForAppSettled(page);

    await page.locator('.agentic-launcher').click();
    await expect(page.locator('.agentic-panel')).toBeVisible();

    await page.keyboard.press('Escape');
    await expect(page.locator('.agentic-panel')).toHaveCount(0);
    expectHealthy(health);
  });

  test('never submits blank drafts and renders hostile payloads inert', async ({page}) => {
    const health = watchPageHealth(page);
    await login(page);
    await waitForAppSettled(page);
    await page.locator('.agentic-launcher').click();
    await expect(page.locator('.agentic-panel')).toBeVisible();

    // let the conversation load settle before snapshotting the count
    await page.waitForTimeout(1500);
    const before = await page.locator('.agentic-message').count();
    await composer(page).fill('   ');
    await composer(page).press('Enter');
    await page.waitForTimeout(1200);
    await expect(page.locator('.agentic-message')).toHaveCount(before);

    const payload = '<img src=x onerror="window.__pwned=1"> hostile **markdown**';
    await composer(page).fill(payload);
    await composer(page).press('Enter');
    // the user bubble shows the raw text; no script executes
    await expect(page.locator('.agentic-message--user').last()).toContainText('hostile');
    await page.waitForTimeout(500);
    expect(await page.evaluate(() => (window as unknown as {__pwned?: number}).__pwned)).toBeUndefined();
    expectHealthy(health);
  });

  test('records workbench state in the URL and restores it after reload', async ({page}, testInfo) => {
    const health = watchPageHealth(page);
    const isMobile = testInfo.project.name.includes('mobile');
    await login(page);
    await waitForAppSettled(page);
    await page.locator('.agentic-launcher').click();
    await expect(page.locator('.agentic-panel')).toBeVisible();

    // mobile hides the workbench entry by design — assert that contract too
    if (isMobile) {
      await expect(page.getByRole('button', {name: /全屏|full.?screen/i})).toHaveCount(0);
      expectHealthy(health);
      return;
    }
    // enter workbench mode
    const expand = page.getByRole('button', {name: /全屏|full.?screen/i}).first();
    await expand.click();
    await expect(page.locator('.agentic-panel--expanded')).toBeVisible();

    // pick a session so the URL records the selection too
    const item = page.locator('.agentic-sessions__item-main').first();
    if (await item.count()) await item.click();
    await page.waitForTimeout(600);
    expect(page.url()).toMatch(/agentic=workbench/);
    expect(page.url()).toMatch(/session=/);

    await page.reload();
    await waitForAppSettled(page);
    await expect(page.locator('.agentic-panel--expanded')).toBeVisible({timeout: 15000});
    expectHealthy(health);
  });

  test('keeps the layout invariants: one icon axis and equal control heights', async ({page}, testInfo) => {
    const health = watchPageHealth(page);
    const isMobile = testInfo.project.name.includes('mobile');
    await login(page);
    await waitForAppSettled(page);
    await page.locator('.agentic-launcher').click();
    await expect(page.locator('.agentic-panel')).toBeVisible();

    // every composer control sits on the same height: attach / model / send
    const heights = await page.evaluate(() => {
      const h = (sel: string) => {
        const el = document.querySelector(sel);
        return el ? Math.round(el.getBoundingClientRect().height) : null;
      };
      return {
        attach: h('.agentic-composer__left button'),
        pill: h('.agentic-model-pill'),
        send: h('.agentic-send'),
      };
    });
    expect(heights.pill).toBe(heights.send);
    expect(heights.attach).toBe(heights.send);

    if (isMobile) {
      // mobile is the docked dialog: the panel spans the viewport width
      const box = await page.locator('.agentic-panel').boundingBox();
      const viewportWidth = page.viewportSize()?.width ?? 0;
      expect(Math.abs(Math.round(box?.width ?? 0) - viewportWidth)).toBeLessThanOrEqual(4);
      expectHealthy(health);
      return;
    }

    // workbench: the rail icons share one vertical axis; input aligns with the stream
    await page.getByRole('button', {name: /全屏|full.?screen/i}).first().click();
    await expect(page.locator('.agentic-panel--expanded')).toBeVisible();
    await page.waitForTimeout(MORPH_SETTLE_MS);
    const geometry = await readingColumnGeometry(page);
    // expanded rail: every conversation icon shares one left axis
    const itemIcons = await page.evaluate(() =>
      [...document.querySelectorAll('.agentic-sessions__view .agentic-sessions__item-icon')]
        .filter((el) => el.getBoundingClientRect().width > 0)
        .map((el) => {
          const r = el.getBoundingClientRect();
          return Math.round(r.x + r.width / 2);
        })
    );
    if (itemIcons.length > 1) {
      expect(Math.max(...itemIcons) - Math.min(...itemIcons)).toBeLessThanOrEqual(2);
    }
    expectColumnOnChatLane(geometry);

    // collapsed icon rail: every control shares the vertical centre axis
    await page.locator('.agentic-sessions__collapse').click();
    await expect(page.locator('.agentic-sessions')).toHaveClass(/is-collapsed/);
    await page.waitForTimeout(MORPH_SETTLE_MS);
    const collapsedCenters = await page.evaluate(() => {
      const rail = document.querySelector('.agentic-sessions');
      return [...(rail?.querySelectorAll('button') ?? [])]
        .filter((el) => el.getBoundingClientRect().width > 0) // skip hidden ⋯ menus
        .map((el) => {
          const r = el.getBoundingClientRect();
          return Math.round(r.x + r.width / 2);
        });
    });
    expect(collapsedCenters.length).toBeGreaterThanOrEqual(3);
    expect(Math.max(...collapsedCenters) - Math.min(...collapsedCenters)).toBeLessThanOrEqual(2);
    await page.locator('.agentic-sessions__collapse').click();
    await expect(page.locator('.agentic-sessions')).not.toHaveClass(/is-collapsed/);
    await page.waitForTimeout(MORPH_SETTLE_MS);
    // the collapse round-trip must land back on the same reading-column axis
    expectColumnOnChatLane(await readingColumnGeometry(page));
    expectHealthy(health);
  });

  test('renames a conversation through the rail item menu', async ({page}, testInfo) => {
    const health = watchPageHealth(page);
    const isMobile = testInfo.project.name.includes('mobile');
    await login(page);
    await waitForAppSettled(page);
    await page.locator('.agentic-launcher').click();
    if (isMobile) {
      // no workbench rail on mobile — the lane stays mounted for the panel's
      // lifetime (overlay lanes toggle through CSS visibility), so the docked
      // dialog contract is "never visible", not "absent from the DOM"
      await expect(page.locator('.agentic-sessions')).toBeHidden();
      expectHealthy(health);
      return;
    }
    await page.getByRole('button', {name: /全屏|full.?screen/i}).first().click();
    await expect(page.locator('.agentic-panel--expanded')).toBeVisible();

    const item = page.locator('.agentic-sessions__item').first();
    await item.hover();
    await item.locator('.agentic-sessions__item-more').click();
    // the item menus are lazy (persistent=false): the popper only mounts on
    // first open, so wait for it before driving its items
    await expect(page.locator('.agentic-item-menu')).toBeVisible();
    await page.getByRole('menuitem', {name: /重命名|Rename/}).first().click();
    const dialog = page.locator('.el-dialog:visible, .el-overlay:visible').last();
    await expect(dialog).toBeVisible();
    await dialog.locator('input').first().fill('e2e renamed');
    await dialog.getByRole('button', {name: /保存|[Ss]ave/}).first().click();
    await expect(page.locator('.agentic-sessions__item-main').first()).toContainText('e2e renamed');
    expectHealthy(health);
  });
});
