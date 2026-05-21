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
import io.github.pnoker.common.facade.entity.bo.FacadeResourceRegistrySyncCommandBO;
import io.github.pnoker.common.facade.entity.bo.FacadeResourceRegistrySyncResultBO;
import io.github.pnoker.common.facade.entity.bo.FacadeScannedApiBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceRegistryLocalFacadeTest {

    @Mock
    private ResourceRegistrySyncService resourceRegistrySyncService;

    private ResourceRegistryLocalFacade facade;

    private static FacadeScannedApiBO api(String method, String path, String name, String group) {
        return FacadeScannedApiBO.builder()
                .method(method)
                .path(path)
                .apiName(name)
                .title(method.toLowerCase())
                .remark("")
                .apiGroup(group)
                .build();
    }

    private static <T> T any(Class<T> clazz) {
        return org.mockito.ArgumentMatchers.any(clazz);
    }

    @BeforeEach
    void setUp() {
        facade = new ResourceRegistryLocalFacade(resourceRegistrySyncService);
    }

    @Test
    void syncMapsCommandFieldsAndForwardsApisToSyncService() {
        FacadeResourceRegistrySyncCommandBO commandBO = FacadeResourceRegistrySyncCommandBO.builder()
                .serviceName("dc3-center-auth")
                .deleteMissing(true)
                .apis(List.of(api("GET", "/probe", "Sample.get", "Sample"),
                        api("POST", "/v3/point/write", "Point.write", "Point")))
                .build();

        when(resourceRegistrySyncService.sync(any(ResourceRegistrySyncCommand.class)))
                .thenReturn(ResourceRegistrySyncResult.builder()
                        .inserted(1)
                        .updated(2)
                        .deleted(3)
                        .unchanged(4)
                        .build());

        FacadeResourceRegistrySyncResultBO result = facade.sync(commandBO);

        ArgumentCaptor<ResourceRegistrySyncCommand> captor =
                ArgumentCaptor.forClass(ResourceRegistrySyncCommand.class);
        org.mockito.Mockito.verify(resourceRegistrySyncService).sync(captor.capture());
        ResourceRegistrySyncCommand passed = captor.getValue();
        assertThat(passed.getServiceName()).isEqualTo("dc3-center-auth");
        assertThat(passed.isDeleteMissing()).isTrue();
        assertThat(passed.getApis()).hasSize(2);
        ResourceRegistryScannedApi first = passed.getApis().get(0);
        assertThat(first.getMethod()).isEqualTo("GET");
        assertThat(first.getPath()).isEqualTo("/probe");
        assertThat(first.getApiName()).isEqualTo("Sample.get");
        assertThat(first.getApiGroup()).isEqualTo("Sample");

        assertThat(result.getInserted()).isEqualTo(1);
        assertThat(result.getUpdated()).isEqualTo(2);
        assertThat(result.getDeleted()).isEqualTo(3);
        assertThat(result.getUnchanged()).isEqualTo(4);
    }

    @Test
    void syncReplacesNullApiListWithEmpty() {
        FacadeResourceRegistrySyncCommandBO commandBO = FacadeResourceRegistrySyncCommandBO.builder()
                .serviceName("svc")
                .deleteMissing(false)
                .apis(null)
                .build();
        when(resourceRegistrySyncService.sync(any(ResourceRegistrySyncCommand.class)))
                .thenReturn(ResourceRegistrySyncResult.builder().build());

        facade.sync(commandBO);

        ArgumentCaptor<ResourceRegistrySyncCommand> captor =
                ArgumentCaptor.forClass(ResourceRegistrySyncCommand.class);
        org.mockito.Mockito.verify(resourceRegistrySyncService).sync(captor.capture());
        assertThat(captor.getValue().getApis()).isEmpty();
    }

    @Test
    void syncReplacesEmptyApiListWithEmpty() {
        FacadeResourceRegistrySyncCommandBO commandBO = FacadeResourceRegistrySyncCommandBO.builder()
                .serviceName("svc")
                .deleteMissing(false)
                .apis(List.of())
                .build();
        when(resourceRegistrySyncService.sync(any(ResourceRegistrySyncCommand.class)))
                .thenReturn(ResourceRegistrySyncResult.builder().build());

        facade.sync(commandBO);

        ArgumentCaptor<ResourceRegistrySyncCommand> captor =
                ArgumentCaptor.forClass(ResourceRegistrySyncCommand.class);
        org.mockito.Mockito.verify(resourceRegistrySyncService).sync(captor.capture());
        assertThat(captor.getValue().getApis()).isEmpty();
    }
}
