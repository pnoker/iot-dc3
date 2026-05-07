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

package io.github.pnoker.common.data.entity.vo.dashboard;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * One row in the alert list panel on the home page. Source is either {@code device} (with
 * point_id) or {@code driver}.
 *
 * @author pnoker
 * @since 2026.5.2
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class AlertItemVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private Long id;

	private String source;

	private Long sourceId;

	private Long pointId;

	private Integer eventTypeFlag;

	private Integer confirmFlag;

	private LocalDateTime createTime;

	/**
	 * Human-readable message extracted from event_ext->>'content'. Populated by the
	 * paging / list endpoints; latest(size=N) leaves it null.
	 */
	private String message;

}
