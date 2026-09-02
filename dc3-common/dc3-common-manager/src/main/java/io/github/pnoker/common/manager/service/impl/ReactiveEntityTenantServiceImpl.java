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
package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ReactiveEntityTenantServiceImpl implements ReactiveEntityTenantService {
    private final ReactiveDriverService drivers;
    private final ReactiveProfileService profiles;
    private final ReactivePointService points;
    private final ReactiveDeviceService devices;

    public Mono<Void> requireEntityTenant(Long tenant, EntityTypeEnum type, Long id) {
        if (type == null) return Mono.error(new NotFoundException("Resource does not exist"));
        return switch (type) {
            case DRIVER -> drivers.getById(tenant, id).then();
            case PROFILE -> profiles.getById(tenant, id).then();
            case POINT -> points.getById(tenant, id).then();
            case DEVICE -> devices.getById(tenant, id).then();
            default -> Mono.error(new NotFoundException("Resource does not exist"));
        };
    }
}
