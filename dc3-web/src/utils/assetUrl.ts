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

/**
 * Resolve a `public/` asset path against the deployment base URL.
 *
 * Vite rewrites build-time asset imports for `base`, but NOT verbatim
 * `/images/...` strings written in templates — those resolve against the host
 * root and 404 under a project-page subpath (e.g. `<user>.github.io/iot-dc3/`).
 * Building the URL at runtime via `import.meta.env.BASE_URL` makes the same
 * markup work for root-domain and subpath deployments alike.
 */
export const assetUrl = (path: string): string => `${import.meta.env.BASE_URL}${path}`;
