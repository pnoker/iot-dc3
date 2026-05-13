-- Copyright 2016-present the IoT DC3 original author or authors.
--
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Affero General Public License as
-- published by the Free Software Foundation, either version 3 of the
-- License, or (at your option) any later version.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU Affero General Public License for more details.
--
-- You should have received a copy of the GNU Affero General Public License
-- along with this program.  If not, see <https://www.gnu.org/licenses/>.

-- Bootstrap script applied to the dc3 test database before each test run.
-- Mirrors the production setup of timescale/timescaledb-ha:pg18 so DAL slice
-- tests see identical extensions and search_path behavior. Per-table DDL is
-- contributed by individual modules under their own src/test/resources/db/.

CREATE EXTENSION IF NOT EXISTS timescaledb;
