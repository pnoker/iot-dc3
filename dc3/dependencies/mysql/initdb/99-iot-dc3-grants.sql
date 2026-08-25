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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

--
-- Grants for the composed application user. The MySQL entrypoint only grants
-- MYSQL_USER rights on MYSQL_DATABASE (dc3_data); the seed splits the platform
-- across five databases, so the application user needs the same set or the
-- cross-database queries (alert dashboards join dc3_manager) fail with
-- permission errors. The username must match MYSQL_USERNAME (default dc3).
--
GRANT ALL PRIVILEGES ON dc3_manager.* TO 'dc3'@'%';
GRANT ALL PRIVILEGES ON dc3_auth.* TO 'dc3'@'%';
GRANT ALL PRIVILEGES ON dc3_data.* TO 'dc3'@'%';
GRANT ALL PRIVILEGES ON dc3_history.* TO 'dc3'@'%';
GRANT ALL PRIVILEGES ON dc3_agentic.* TO 'dc3'@'%';
