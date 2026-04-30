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

import lombok.*;

import java.util.List;

/**
 * Command driving a resource-registry sync call.
 *
 * @author pnoker
 * @version 2026.4.30
 * @since 2026.4.30
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRegistrySyncCommand {

    /**
     * Owning service name, e.g. dc3-center-manager.
     */
    private String serviceName;

    /**
     * Tenant ID the resources are registered under. Use 0 for system-level resources.
     */
    private Long tenantId;

    /**
     * When true, endpoints that exist in the DB but are absent from the current scan
     * are soft-deleted. When false, such endpoints are left untouched.
     */
    private boolean deleteMissing;

    /**
     * Complete list of endpoints discovered by the registrar on the calling service.
     */
    private List<ResourceRegistryScannedApi> apis;
}
