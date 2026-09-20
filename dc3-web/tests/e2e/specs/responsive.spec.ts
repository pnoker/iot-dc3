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

/** Visible component boxes must stay inside the viewport, even when an
 * ancestor intentionally owns scrolling. Page-level scrollWidth alone cannot
 * catch a flex child that is clipped by an overflow-hidden card. */
const expectVisibleBoxesWithinViewport = async (
  page: Page,
  selector: string,
  label: string,
) => {
  const violations = await page.evaluate((target) => {
    const viewport = document.documentElement.clientWidth;
    return [...document.querySelectorAll<HTMLElement>(target)]
      .filter((element) => {
        const style = getComputedStyle(element);
        const rect = element.getBoundingClientRect();
        return (
          style.display !== "none" &&
          style.visibility !== "hidden" &&
          rect.width > 0 &&
          rect.height > 0
        );
      })
      .map((element) => {
        const rect = element.getBoundingClientRect();
        return {
          className: String(element.className).slice(0, 120),
          left: rect.left,
          right: rect.right,
          viewport,
        };
      })
      .filter(({ left, right, viewport }) => left < -1 || right > viewport + 1);
  }, selector);

  expect(violations, label + ": visible box clipped by viewport").toEqual([]);
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

  test("home SLA chips keep compact visuals and the shared control density", async ({
    page,
  }) => {
    await login(page);
    await page.goto("/#/home", { waitUntil: "domcontentloaded" });
    await waitForAppSettled(page);

    const badge = page.locator(".sla-badge");
    await expect(badge).toBeVisible();
    const badgeHeight = await badge.evaluate(
      (element) => element.getBoundingClientRect().height,
    );
    const metrics = await badge.locator(".sla-badge__chip").evaluateAll(
      (chips) =>
        chips.map((chip) => {
          const element = chip as HTMLElement;
          const visual = getComputedStyle(element, "::before");
          const rect = element.getBoundingClientRect();
          return {
            height: rect.height,
            visualHeight: Number.parseFloat(visual.height),
            background: getComputedStyle(element).backgroundColor,
          };
        }),
    );
    // A3 (v7 note): controls share the 32px density on every terminal, so the
    // chips are never enlarged for coarse pointers. The strip bound is that
    // 32px chip plus the badge padding and border.
    expect(metrics.length).toBeGreaterThan(0);
    expect(badgeHeight).toBeLessThanOrEqual(44);
    for (const metric of metrics) {
      expect(metric.visualHeight).toBeGreaterThanOrEqual(26);
      expect(metric.visualHeight).toBeLessThan(metric.height);
      expect(metric.visualHeight).toBeLessThanOrEqual(30);
      expect(metric.background).toMatch(/rgba\(0, 0, 0, 0\)|transparent/);
      expect(metric.height).toBeGreaterThanOrEqual(32);
      expect(metric.height).toBeLessThanOrEqual(36);
    }
  });

  test("action alerts keep the message and retry action compact", async ({
    page,
  }) => {
    await login(page);
    await page.evaluate(() => {
      const fixture = document.createElement("div");
      fixture.id = "responsive-action-alert";
      fixture.className = "el-alert el-alert--error is-light";
      fixture.setAttribute("role", "alert");
      fixture.innerHTML = `
        <i class="el-icon el-alert__icon is-big" aria-hidden="true">
          <svg viewBox="0 0 1024 1024"><path d="M0 0h1024v1024H0z" /></svg>
        </i>
        <div class="el-alert__content">
          <span class="el-alert__title with-description">Unable to load data.</span>
          <p class="el-alert__description">
            <button class="el-button el-button--danger el-button--default is-link" type="button">
              <span>Retry</span>
            </button>
          </p>
        </div>
      `;
      document.querySelector(".home")?.prepend(fixture);
    });

    const alert = page.locator("#responsive-action-alert");
    await expect(alert).toBeVisible();
    const metrics = await alert.evaluate((element) => {
      const content = element.querySelector<HTMLElement>(".el-alert__content")!;
      const title = element.querySelector<HTMLElement>(".el-alert__title")!;
      const action = element.querySelector<HTMLElement>(".el-button")!;
      const titleRect = title.getBoundingClientRect();
      const actionRect = action.getBoundingClientRect();
      return {
        height: element.getBoundingClientRect().height,
        contentDirection: getComputedStyle(content).flexDirection,
        titleCenter: titleRect.top + titleRect.height / 2,
        actionCenter: actionRect.top + actionRect.height / 2,
      };
    });

    expect(metrics.contentDirection).toBe("row");
    expect(Math.abs(metrics.titleCenter - metrics.actionCenter)).toBeLessThanOrEqual(1);
    expect(metrics.height).toBeLessThanOrEqual(40);
    // Retry is a text-link action, so it keeps the shared density instead of a
    // coarse-pointer enlargement (A3 v7 note): it only has to stay usable.
    const retry = alert.locator(".el-button");
    await expect(retry).toBeVisible();
    await expect(retry).toBeEnabled();
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
    await expect(page.locator('.header_settings_button')).toBeVisible();
    await expect(page.locator('.user_trigger')).toBeVisible();

    if (mobile) {
      // The primary menu stays in the capsule as an icon-compact strip —
      // no hamburger, no drawer. Language/theme fold behind the "…" chip.
      await expect(page.locator(".header_menu_toggle")).toHaveCount(0);
      await expect(page.locator(".nav-drawer")).toHaveCount(0);
      await expect(page.locator(".header_menu_wrap")).toBeVisible();
      await expect(page.locator(".nav-menu--compact")).toHaveCount(1);

      // Preferences live in the popover, not flat in the capsule.
      await expect(
        page.locator(".header_actions_glass > * .app-preferences"),
      ).toHaveCount(0);
      await page.locator(".header_more_button").click();
      await expect(page.locator(".app-preferences")).toBeVisible();
      await page.keyboard.press("Escape");

      // Settings: the HEADER settings button bubbles up an arrowed popover
      // with the fully-expanded menu (no aside, no floating corner toggle,
      // no drawer).
      await page.goto("/#/settings/user", { waitUntil: "domcontentloaded" });
      await waitForAppSettled(page);
      await expect(page.locator(".settings-aside")).toHaveCount(0);
      await expect(page.locator(".settings-aside-toggle")).toHaveCount(0);
      await page.locator(".header_settings_button").click();
      await expect(page.locator(".settings-nav-popover")).toBeVisible();
      // Every group arrives unfolded — a child item is visible without
      // any intermediate tap.
      await expect(page.locator(".settings-nav-popover .el-menu-item")).toBeVisible();
    } else {
      // Desktop keeps labels; tablet uses icon-compact navigation. Neither
      // mode falls back to Element Plus's three-dot overflow item.
      await expect(page.locator(".header_menu_toggle")).toBeHidden();
      await expect(page.locator(".header_menu_wrap")).toBeVisible();
      await expect(page.locator('.nav-menu--compact')).toHaveCount(
        isTabletViewport(page) ? 1 : 0,
      );
      await expect(page.locator('.header_actions_glass .app-preferences')).toBeVisible();
      await expect(page.locator('.header_actions_glass .app-preferences__theme')).toBeVisible();

      await page.goto("/#/settings/user", { waitUntil: "domcontentloaded" });
      await waitForAppSettled(page);
      await expect(page.locator(".settings-aside")).toBeVisible();
      if (isTabletViewport(page)) {
        // Tablet uses the compact rail contract rather than spending a third
        // of the viewport on labels that are available in the drawer/menu.
        await expect(page.locator(".settings-aside")).toHaveAttribute(
          "style",
          /width:\s*64px/,
        );
        await expect(page.locator(".settings-sidebar-menu.el-menu--collapse")).toBeVisible();
      }
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

  test("mobile toolbars keep every action reachable", async ({ page }) => {
    if (!isMobileViewport(page)) {
      return;
    }

    await login(page);

    await page.goto("/#/device", { waitUntil: "domcontentloaded" });
    await waitForAppSettled(page);
    await expectVisibleBoxesWithinViewport(
      page,
      ".tool-card__footer, .tool-card-footer-button, .tool-card-footer-page",
      "device toolbar",
    );

    await page.goto("/#/point_value", { waitUntil: "domcontentloaded" });
    await waitForAppSettled(page);
    await expectVisibleBoxesWithinViewport(
      page,
      ".range-segmented",
      "point-value range selector",
    );

    await page.goto("/#/home", { waitUntil: "domcontentloaded" });
    await waitForAppSettled(page);
    await expectVisibleBoxesWithinViewport(
      page,
      ".dashboard-card__tools",
      "dashboard card tools",
    );

    await page.goto("/#/settings/alarm/overview", {
      waitUntil: "domcontentloaded",
    });
    await waitForAppSettled(page);
    await expectVisibleBoxesWithinViewport(
      page,
      ".event-overview__quick, .event-overview__quick-actions, .event-overview__quick-actions button",
      "alarm overview quick actions",
    );

    await page.goto("/#/settings/mcp", { waitUntil: "domcontentloaded" });
    await waitForAppSettled(page);
    await expectVisibleBoxesWithinViewport(
      page,
      ".mcp-overview__copy-line, .el-descriptions__table",
      "MCP metadata",
    );
  });
});
