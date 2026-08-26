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

// Three-terminal gate (docs/design/frontend-three-terminal-ux.md):
// runs under chromium-desktop / chromium-tablet / chromium-mobile projects.
// Gates the A2 acceptance criterion (no page-level horizontal scroll from
// 360px to 2560px), the A3 shell adaptation (hamburger vs menu strip,
// aside vs drawer), and an A7 smoke probe (interactive controls expose
// accessible names). Run: pnpm exec playwright test responsive.spec.ts

import { expect, type Page, test } from "@playwright/test";

import { login, waitForAppSettled } from "../fixtures/app";

/** Page-level overflow must not exceed the viewport (A2 acceptance line). */
const expectNoHorizontalOverflow = async (page: Page, label: string) => {
  const overflow = await page.evaluate(
    () =>
      document.documentElement.scrollWidth -
      document.documentElement.clientWidth,
  );
  expect(
    overflow,
    label + ": page-level horizontal overflow",
  ).toBeLessThanOrEqual(0);
};

const isMobileViewport = (page: Page) =>
  (page.viewportSize()?.width ?? 1440) < 768;

const isTabletViewport = (page: Page) => {
  const width = page.viewportSize()?.width ?? 1440;
  return width >= 768 && width < 1200;
};

test.describe("three-terminal gate", () => {
  test("login, home, and settings keep zero page-level overflow", async ({
    page,
  }) => {
    await expectNoHorizontalOverflow(page, "login");
    await login(page);
    await expectNoHorizontalOverflow(page, "home");

    await page.goto("/#/settings/user", { waitUntil: "domcontentloaded" });
    await waitForAppSettled(page);
    await expectNoHorizontalOverflow(page, "settings");
  });

  // L4 template sweep: one representative route per page template family
  // (monitor / list / detail / history), gated on every terminal viewport.
  const TEMPLATE_ROUTES: Array<{ template: string; hash: string }> = [
    { template: "monitor", hash: "/#/settings/alarm/overview" },
    { template: "list", hash: "/#/device" },
    { template: "list", hash: "/#/driver" },
    { template: "list", hash: "/#/profile" },
    { template: "list", hash: "/#/settings/label" },
    { template: "detail", hash: "/#/point_value" },
    { template: "detail", hash: "/#/settings/alarm/point" },
    { template: "history", hash: "/#/settings/event/history" },
    { template: "history", hash: "/#/settings/command/history" },
  ];

  for (const { template, hash } of TEMPLATE_ROUTES) {
    test(`template ${template} ${hash} keeps zero page-level overflow`, async ({
      page,
    }) => {
      await login(page);
      await page.goto(hash, { waitUntil: "domcontentloaded" });
      await waitForAppSettled(page);
      await expectNoHorizontalOverflow(page, `${template} ${hash}`);
    });
  }

  test("shell adapts to the viewport (menu strip vs drawer, aside vs drawer)", async ({
    page,
  }) => {
    await login(page);
    const mobile = isMobileViewport(page);

    await expect(page.locator('.header_brand_glass')).toHaveCount(1);
    await expect(page.locator('.header_actions_glass')).toHaveCount(1);
    await expect(page.locator('.header_language_switch')).toBeVisible();
    await expect(page.locator('.header_settings_button')).toBeVisible();
    await expect(page.locator('.user_trigger')).toBeVisible();

    if (mobile) {
      // Hamburger replaces the horizontal menu strip.
      await expect(page.locator(".header_menu_toggle")).toBeVisible();
      await expect(page.locator(".header_menu_wrap")).toHaveCount(0);

      // Drawer opens and carries the navigation tree.
      await page.locator(".header_menu_toggle").click();
      await expect(page.locator(".nav-drawer")).toBeVisible();
      await expect(page.locator(".nav-drawer")).toContainText("Home");
      await page.keyboard.press("Escape");

      // Settings: floating toggle + drawer replace the fixed aside.
      await page.goto("/#/settings/user", { waitUntil: "domcontentloaded" });
      await waitForAppSettled(page);
      await expect(page.locator(".settings-aside")).toHaveCount(0);
      await expect(page.locator(".settings-aside-toggle")).toBeVisible();
      await page.locator(".settings-aside-toggle").click();
      await expect(page.locator(".settings-drawer")).toBeVisible();
    } else {
      // Desktop keeps labels; tablet uses icon-compact navigation. Neither
      // mode falls back to Element Plus's three-dot overflow item.
      await expect(page.locator(".header_menu_toggle")).toBeHidden();
      await expect(page.locator(".header_menu_wrap")).toBeVisible();
      await expect(page.locator('.nav-menu--compact')).toHaveCount(
        isTabletViewport(page) ? 1 : 0,
      );

      await page.goto("/#/settings/user", { waitUntil: "domcontentloaded" });
      await waitForAppSettled(page);
      await expect(page.locator(".settings-aside")).toBeVisible();
    }
  });

  test("a11y smoke: visible interactive controls expose accessible names", async ({
    page,
  }) => {
    await login(page);
    await page.goto("/#/settings/user", { waitUntil: "domcontentloaded" });
    await waitForAppSettled(page);

    const unlabeled = await page.evaluate(() => {
      const out: Array<{ cls: string; text: string }> = [];
      document.querySelectorAll('button, [role="button"]').forEach((el) => {
        const node = el as HTMLElement;
        if (node.offsetParent === null) return; // hidden
        const name =
          node.getAttribute("aria-label") ||
          node.getAttribute("title") ||
          node.textContent?.trim() ||
          "";
        if (!name) {
          out.push({
            cls: String(node.className).slice(0, 80),
            text: (node.textContent || "").slice(0, 60),
          });
        }
      });
      return out;
    });

    expect(unlabeled, "visible controls without an accessible name").toEqual(
      [],
    );
  });

  // A3 acceptance: dialogs go (near) full-screen below 768px, so a fixed
  // 640px dialog width never overflows a mobile viewport. Gated on mobile
  // only — desktop/tablet keep the centered width contract.
  test("dialogs fit the mobile viewport below 768px", async ({ page }) => {
    if (!isMobileViewport(page)) {
      return;
    }
    await login(page);

    await page.goto("/#/settings/label", { waitUntil: "domcontentloaded" });
    await waitForAppSettled(page);
    await page
      .getByRole("button", { name: /Add|新增/ })
      .first()
      .click();
    const dialog = page.locator(".el-dialog:visible").last();
    await expect(dialog).toBeVisible();

    const overflow = await page.evaluate(
      () =>
        document.documentElement.scrollWidth -
        document.documentElement.clientWidth,
    );
    expect(
      overflow,
      "open dialog causes page-level horizontal overflow",
    ).toBeLessThanOrEqual(0);
    const dialogWidth = await dialog.boundingBox();
    const viewportWidth = page.viewportSize()?.width ?? 393;
    expect(
      dialogWidth?.width,
      "dialog wider than viewport",
    ).toBeLessThanOrEqual(viewportWidth);
  });
});
