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

package io.github.pnoker.common.facade.local.config;

import io.github.pnoker.common.facade.local.PointCommandLocalFacade;
import io.github.pnoker.common.facade.local.PointValueLocalFacade;
import io.github.pnoker.common.facade.local.StatusHealthLocalFacade;
import io.github.pnoker.common.facade.local.builder.FacadePointValueBuilder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Auto-configuration for the data-domain local facade implementations. Active only when
 * {@code dc3.facade.mode=local}. Scans the facade-local package but restricts
 * registration to the data-domain classes carried by this module so it remains
 * deterministic alongside the auth/manager-domain modules. MapStruct-generated
 * {@code *BuilderImpl} classes are picked up via their interfaces (AssignableTypeFilter
 * matches concrete implementations).
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@AutoConfiguration
@ConditionalOnProperty(name = "dc3.facade.mode", havingValue = "local")
@ComponentScan(basePackages = "io.github.pnoker.common.facade.local", useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                PointValueLocalFacade.class, PointCommandLocalFacade.class, StatusHealthLocalFacade.class,
                FacadePointValueBuilder.class,}))
public class LocalFacadeDataAutoConfiguration {

    /**
     * Referenced only to ensure the {@link PointValueLocalFacade} symbol is linked
     * against this module's compiled classes at AutoConfiguration load time.
     */
    @SuppressWarnings("unused")
    private static final Class<?> CANARY = PointValueLocalFacade.class;

}
