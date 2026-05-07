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

package io.github.pnoker.common.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Postgres session-level advisory lock helper used by the resource registry sync flow to
 * serialize concurrent registrations of the same service.
 *
 * @author pnoker
 * @version 2026.5.5
 * @since 2026.5.5
 */
@Mapper
public interface ResourceRegistryLockMapper {

	/**
	 * Acquire a transaction-scoped advisory lock keyed by the given string. The lock is
	 * automatically released when the enclosing transaction ends.
	 *
	 * <p>
	 * SQL lives in {@code resources/mapping/ResourceRegistryLockMapper.xml}. The return
	 * value is an empty string and is intentionally discarded by callers; it only exists
	 * so MyBatis can map the ResultSet produced by the {@code SELECT} statement.
	 * PostgreSQL's {@code pg_advisory_xact_lock} returns {@code void}, which is cast to
	 * {@code text} in the XML mapping to satisfy MyBatis' result-mapping contract.
	 */
	String advisoryLock(@Param("key") String key);

}
