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

package io.github.pnoker.common.auth.entity.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A single HTTP endpoint discovered by the resource registrar scanner.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRegistryScannedApi {

    /**
     * HTTP method: GET / POST / PUT / DELETE.
     */
    private String method;

    /**
     * Full path (post gateway strip), e.g. /device/add.
     */
    private String path;

    /**
     * Resource code fragment in {@code domain:scope} format, e.g. {@code device:get}.
     * The full resource code is composed by prefixing the service name at sync time.
     */
    private String apiName;

    /**
     * Short title, usually the controller method name.
     */
    private String title;

    /**
     * Optional description.
     */
    private String remark;

    /**
     * API grouping label — usually the owning controller's simple class name, e.g.
     * "ApiController". Endpoints sharing the same apiGroup become siblings under the same
     * resource-tree node.
     */
    private String apiGroup;

}
