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

package io.github.pnoker.common.data.biz;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.query.PointValueQuery;

import java.util.List;

/**
 * Interface for point value-related operations
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public interface PointValueService {

	/**
	 * Save point value
	 * @param pointValueBO PointValue
	 */
	void save(PointValueBO pointValueBO);

	/**
	 * Batch save point values
	 * @param pointValueBOList Array
	 */
	void save(List<PointValueBO> pointValueBOList);

	/**
	 * Get historical point values within the tenant scope.
	 * @param tenantId Tenant ID (required for tenant isolation)
	 * @param deviceId Device ID
	 * @param pointId Point ID
	 * @param count Number of values to retrieve
	 * @return History Value Array
	 */
	List<String> history(Long tenantId, Long deviceId, Long pointId, int count);

	/**
	 * Get latest point values with pagination and sorting
	 * @param pointValueQuery Entry of Query
	 * @return Entity of BO Page
	 */
	Page<PointValueBO> latest(PointValueQuery pointValueQuery);

	/**
	 * Get point values with pagination and sorting
	 * @param pointValueQuery Entry of Query
	 * @return Entity of BO Page
	 */
	Page<PointValueBO> page(PointValueQuery pointValueQuery);

}
