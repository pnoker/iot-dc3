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

import {on} from '../dispatch';
import {ok, responseOf} from '../response';

/**
 * Mock the token endpoints so the real login/logout/change-password flow runs
 * end-to-end. The auth store reads saltRes.data / tokenRes.data as plain
 * strings, so we return R<string> envelopes.
 */
export function registerAuthHandlers(): void {
  on('post', 'api/v3/auth/token/salt', (ctx) => responseOf(ctx.config, ok('mock-salt')));
  on('post', 'api/v3/auth/token/generate', (ctx) => responseOf(ctx.config, ok('mock-token')));
  on('post', 'api/v3/auth/token/check', (ctx) => responseOf(ctx.config, ok(true)));
  on('post', 'api/v3/auth/token/cancel', (ctx) => responseOf(ctx.config, ok(true)));
  on('post', 'api/v3/auth/token/change_password', (ctx) => responseOf(ctx.config, ok(true)));
}
