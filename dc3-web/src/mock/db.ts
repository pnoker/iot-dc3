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
 * Mutable in-memory store. add/update/delete handlers mutate these arrays so a
 * demo session reflects user actions without a backend. Seeded by the
 * `seed/*` modules at adapter install time.
 */
export interface MockDb {
  drivers: Record<string, unknown>[];
  devices: Record<string, unknown>[];
  profiles: Record<string, unknown>[];
  points: Record<string, unknown>[];
}

export const db: MockDb = {
  drivers: [],
  devices: [],
  profiles: [],
  points: [],
};
