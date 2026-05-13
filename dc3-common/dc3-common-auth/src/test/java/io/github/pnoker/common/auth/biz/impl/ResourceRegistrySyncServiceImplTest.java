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

package io.github.pnoker.common.auth.biz.impl;

import io.github.pnoker.common.auth.dal.ApiManager;
import io.github.pnoker.common.auth.dal.ResourceManager;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistryScannedApi;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistrySyncCommand;
import io.github.pnoker.common.auth.mapper.ResourceRegistryLockMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Lightweight contract for the parameter-validation surface of the registry
 * reconciler. Behavioural coverage of the full sync flow (insert / update /
 * delete / grouping-node lifecycle) is delivered as a Testcontainers slice
 * test in a later stage where the real PostgreSQL schema is available; here
 * the focus is on the cheap fail-fast paths that protect the lock + advisory
 * mapper from being called with invalid input.
 */
@ExtendWith(MockitoExtension.class)
class ResourceRegistrySyncServiceImplTest {

    @Mock
    private ApiManager apiManager;

    @Mock
    private ResourceManager resourceManager;

    @Mock
    private ResourceRegistryLockMapper resourceRegistryLockMapper;

    @InjectMocks
    private ResourceRegistrySyncServiceImpl service;

    @Test
    void syncRejectsNullCommandBeforeAcquiringAdvisoryLock() {
        assertThatThrownBy(() -> service.sync(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("serviceName");
        verifyNoInteractions(resourceRegistryLockMapper, apiManager, resourceManager);
    }

    @Test
    void syncRejectsBlankServiceName() {
        ResourceRegistrySyncCommand command = new ResourceRegistrySyncCommand();
        command.setServiceName("   ");
        assertThatThrownBy(() -> service.sync(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("serviceName");
        verifyNoInteractions(resourceRegistryLockMapper);
    }

    @Test
    void syncRejectsScannedApiWithBlankMethod() {
        ResourceRegistryScannedApi spec = new ResourceRegistryScannedApi();
        spec.setPath("/api/foo");
        ResourceRegistrySyncCommand command = new ResourceRegistrySyncCommand();
        command.setServiceName("manager");
        command.setApis(List.of(spec));
        assertThatThrownBy(() -> service.sync(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("method");
    }

    @Test
    void syncRejectsScannedApiWithUnsupportedMethod() {
        ResourceRegistryScannedApi spec = new ResourceRegistryScannedApi();
        spec.setMethod("OPTIONS");
        spec.setPath("/api/foo");
        ResourceRegistrySyncCommand command = new ResourceRegistrySyncCommand();
        command.setServiceName("manager");
        command.setApis(List.of(spec));
        assertThatThrownBy(() -> service.sync(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported HTTP method");
    }

    @Test
    void syncRejectsScannedApiWithBlankPath() {
        ResourceRegistryScannedApi spec = new ResourceRegistryScannedApi();
        spec.setMethod("GET");
        spec.setPath(" ");
        ResourceRegistrySyncCommand command = new ResourceRegistrySyncCommand();
        command.setServiceName("manager");
        command.setApis(List.of(spec));
        assertThatThrownBy(() -> service.sync(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path");
    }

    @Test
    void removeMenuResourceIsNullSafe() {
        assertThatNoException().isThrownBy(() -> service.removeMenuResource(null));
        verifyNoInteractions(resourceManager);
    }

    @Test
    void syncMenuResourceIgnoresNullMenu() {
        assertThatNoException().isThrownBy(() -> service.syncMenuResource(null));
        verifyNoInteractions(resourceManager);
    }
}
