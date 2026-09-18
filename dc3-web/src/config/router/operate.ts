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

import type {RouteRecordRaw} from 'vue-router';

/**
 * Operation routes are defined beside their stable layout parent in
 * `views.ts` and `settings.ts`. Keeping this module as an empty route group
 * preserves the router assembly contract without registering duplicate
 * `/driver`, `/device`, `/profile`, `/point`, or `/settings` parents.
 */
const routes: Array<RouteRecordRaw> = [];

export default routes;
