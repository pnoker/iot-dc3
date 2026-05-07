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

package io.github.pnoker.common.handler;

import io.github.pnoker.common.constant.common.TimeConstant;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Bridges {@link LocalDateTime} entity fields and PostgreSQL {@code TIMESTAMPTZ} columns.
 * <p>
 * PostgreSQL JDBC 42.7+ refuses to coerce {@code TIMESTAMPTZ} directly into
 * {@link LocalDateTime}; we read via {@link OffsetDateTime} and convert to the platform's
 * {@link TimeConstant#DEFAULT_ZONEID}. Writes go out as {@link OffsetDateTime} anchored
 * to the same zone so round-trips are stable. This replaces MyBatis's built-in
 * {@code LocalDateTimeTypeHandler}.
 */
@MappedTypes(LocalDateTime.class)
public class TimestamptzLocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType)
			throws SQLException {
		ps.setObject(i, parameter.atZone(TimeConstant.DEFAULT_ZONEID).toOffsetDateTime());
	}

	@Override
	public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return toLocalDateTime(rs.getObject(columnName, OffsetDateTime.class));
	}

	@Override
	public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return toLocalDateTime(rs.getObject(columnIndex, OffsetDateTime.class));
	}

	@Override
	public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return toLocalDateTime(cs.getObject(columnIndex, OffsetDateTime.class));
	}

	private LocalDateTime toLocalDateTime(OffsetDateTime odt) {
		return odt == null ? null : odt.atZoneSameInstant(TimeConstant.DEFAULT_ZONEID).toLocalDateTime();
	}

}
