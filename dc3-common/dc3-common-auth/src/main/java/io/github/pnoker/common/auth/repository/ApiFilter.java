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
package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.enums.ApiTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
/** Tenant-scoped query filter for api records. */

public record ApiFilter(
        String serviceName,
        ApiTypeEnum apiTypeFlag,
        String apiName,
        String apiCode,
        String apiGroup,
        EnableFlagEnum enableFlag,
        PageRequest page) {
    public ApiFilter {
        page = page == null ? PageRequest.firstPage() : page;
        serviceName = normalize(serviceName);
        apiName = normalize(apiName);
        apiCode = normalize(apiCode);
        apiGroup = normalize(apiGroup);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
