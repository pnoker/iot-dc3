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

package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.auth.biz.ResourceRegistrySyncService;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistryScannedApi;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistrySyncCommand;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistrySyncResult;
import io.github.pnoker.common.facade.api.ResourceRegistryFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeResourceRegistrySyncCommandBO;
import io.github.pnoker.common.facade.entity.bo.FacadeResourceRegistrySyncResultBO;
import io.github.pnoker.common.facade.entity.bo.FacadeScannedApiBO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * In-process {@link ResourceRegistryFacade}. Used when the calling service and the auth
 * service live in the same JVM (center-auth self-registration, single-node).
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceRegistryLocalFacade implements ResourceRegistryFacade {

    private final ResourceRegistrySyncService resourceRegistrySyncService;

    private static List<ResourceRegistryScannedApi> toScannedApis(List<FacadeScannedApiBO> apis) {
        if (Objects.isNull(apis) || apis.isEmpty()) {
            return List.of();
        }
        List<ResourceRegistryScannedApi> out = new ArrayList<>(apis.size());
        for (FacadeScannedApiBO bo : apis) {
            out.add(ResourceRegistryScannedApi.builder()
                    .method(bo.getMethod())
                    .path(bo.getPath())
                    .apiName(bo.getApiName())
                    .title(bo.getTitle())
                    .remark(bo.getRemark())
                    .apiGroup(bo.getApiGroup())
                    .build());
        }
        return out;
    }

    @Override
    public FacadeResourceRegistrySyncResultBO sync(FacadeResourceRegistrySyncCommandBO command) {
        ResourceRegistrySyncCommand cmd = ResourceRegistrySyncCommand.builder()
                .serviceName(command.getServiceName())
                .deleteMissing(command.isDeleteMissing())
                .apis(toScannedApis(command.getApis()))
                .build();
        ResourceRegistrySyncResult result = resourceRegistrySyncService.sync(cmd);
        return FacadeResourceRegistrySyncResultBO.builder()
                .inserted(result.getInserted())
                .updated(result.getUpdated())
                .deleted(result.getDeleted())
                .unchanged(result.getUnchanged())
                .build();
    }

}
