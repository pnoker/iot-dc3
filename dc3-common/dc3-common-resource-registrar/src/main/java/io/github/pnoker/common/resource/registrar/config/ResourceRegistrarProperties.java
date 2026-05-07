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

package io.github.pnoker.common.resource.registrar.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the resource registrar.
 *
 * @author pnoker
 * @since 2026.5.5
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "dc3.resource-registrar")
public class ResourceRegistrarProperties {

    /**
     * Master switch. When false, the registrar is a no-op.
     */
    private boolean enabled = true;

    /**
     * Owning service name. Defaults to {@code spring.application.name} when blank.
     */
    private String serviceName;

    /**
     * When true, endpoints that exist in the DB but are absent from the current scan are
     * soft-deleted. Leave false in production to avoid accidental wipes during rolling
     * deploys of older binaries.
     */
    private boolean deleteMissing = true;

    /**
     * Path patterns (Ant-style) to exclude from the scan, in addition to the built-in
     * actuator/error defaults.
     */
    private List<String> excludePaths = new ArrayList<>();

    /**
     * When true, a registration failure aborts startup. When false, it is logged and
     * startup continues.
     */
    private boolean failFast = false;

}
