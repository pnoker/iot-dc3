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
package io.github.pnoker.common.manager.grpc;

import io.github.pnoker.api.common.PageRequest;
import io.github.pnoker.api.common.SortDirection;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import java.util.List;

/** Converts the wire page contract without silently repairing invalid input. */
public final class GrpcPageUtil {

    private GrpcPageUtil() {}

    public static io.github.pnoker.db.r2dbc.core.page.PageRequest require(PageRequest page) {
        if (page == null) {
            throw new IllegalArgumentException("page is required");
        }
        return new io.github.pnoker.db.r2dbc.core.page.PageRequest(page.getOffset(), page.getLimit(), sort(page));
    }

    public static List<SortSpec> sort(PageRequest page) {
        if (page == null) {
            throw new IllegalArgumentException("page is required");
        }
        return page.getSortList().stream()
                .map(spec -> {
                    if (spec.getField().isBlank() || spec.getDirection() == SortDirection.SORT_DIRECTION_UNSPECIFIED) {
                        throw new IllegalArgumentException("sort field and direction are required");
                    }
                    return new SortSpec(
                            spec.getField(),
                            spec.getDirection() == SortDirection.SORT_DIRECTION_DESC
                                    ? SortSpec.Direction.DESC
                                    : SortSpec.Direction.ASC);
                })
                .toList();
    }
}
