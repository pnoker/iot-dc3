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

// Augment vue-router's RouteMeta so route definitions under src/config/router/**
// can use `meta: { title, icon }` with full type-checking.
import 'vue-router';

declare module 'vue-router' {
  interface RouteMeta {
    /** Page title used in document.title and menu labels */
    title?: string;
    /** Global Element Plus icon name (e.g. 'Promotion', 'List') used in the nav menu */
    icon?: string;
  }
}

export {};
