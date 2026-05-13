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

package io.github.pnoker.common.resource.registrar;

import io.github.pnoker.common.facade.api.ResourceRegistryFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeResourceRegistrySyncCommandBO;
import io.github.pnoker.common.facade.entity.bo.FacadeResourceRegistrySyncResultBO;
import io.github.pnoker.common.facade.entity.bo.FacadeScannedApiBO;
import io.github.pnoker.common.resource.registrar.config.ResourceRegistrarProperties;
import io.github.pnoker.common.resource.registrar.scan.ApiEndpointScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceRegistrarTest {

    @Mock
    private ApiEndpointScanner scanner;

    @Mock
    private ResourceRegistryFacade facade;

    @Mock
    private Environment environment;

    private ResourceRegistrarProperties properties;
    private ResourceRegistrar registrar;

    @BeforeEach
    void setUp() {
        properties = new ResourceRegistrarProperties();
        registrar = new ResourceRegistrar(scanner, facade, properties, environment);
    }

    @Test
    void disabledRegistrarSkipsScanAndSync() {
        properties.setEnabled(false);
        registrar.register();
        verifyNoInteractions(scanner, facade, environment);
    }

    @Test
    void blankServiceNameWithFailFastThrowsIllegalState() {
        properties.setEnabled(true);
        properties.setFailFast(true);
        properties.setServiceName(null);
        when(environment.getProperty("spring.application.name")).thenReturn(null);

        assertThatThrownBy(() -> registrar.register())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("service name");
        verifyNoInteractions(scanner, facade);
    }

    @Test
    void blankServiceNameWithoutFailFastWarnsAndReturns() {
        properties.setEnabled(true);
        properties.setFailFast(false);
        properties.setServiceName("   ");
        when(environment.getProperty("spring.application.name")).thenReturn("");

        assertThatNoException().isThrownBy(() -> registrar.register());
        verifyNoInteractions(scanner, facade);
    }

    @Test
    void propertiesServiceNameTakesPrecedenceOverEnvironment() {
        properties.setServiceName("dc3-center-auth");
        properties.setDeleteMissing(true);
        when(scanner.scan()).thenReturn(List.of(api("GET", "/probe")));
        when(facade.sync(any())).thenReturn(result(1, 0, 0, 0));

        registrar.register();

        ArgumentCaptor<FacadeResourceRegistrySyncCommandBO> captor =
                ArgumentCaptor.forClass(FacadeResourceRegistrySyncCommandBO.class);
        verify(facade).sync(captor.capture());
        assertThat(captor.getValue().getServiceName()).isEqualTo("dc3-center-auth");
        assertThat(captor.getValue().isDeleteMissing()).isTrue();
        verify(environment, never()).getProperty(any(String.class));
    }

    @Test
    void fallsBackToSpringApplicationNameWhenPropertyBlank() {
        properties.setServiceName(null);
        when(environment.getProperty("spring.application.name")).thenReturn("dc3-center-data");
        when(scanner.scan()).thenReturn(List.of());
        when(facade.sync(any())).thenReturn(result(0, 0, 0, 0));

        registrar.register();

        ArgumentCaptor<FacadeResourceRegistrySyncCommandBO> captor =
                ArgumentCaptor.forClass(FacadeResourceRegistrySyncCommandBO.class);
        verify(facade).sync(captor.capture());
        assertThat(captor.getValue().getServiceName()).isEqualTo("dc3-center-data");
    }

    @Test
    void happyPathPassesScannedEndpointsThroughToFacade() {
        properties.setServiceName("svc");
        FacadeScannedApiBO probe = api("GET", "/probe");
        FacadeScannedApiBO write = api("POST", "/v3/point/write");
        when(scanner.scan()).thenReturn(List.of(probe, write));
        when(facade.sync(any())).thenReturn(result(2, 0, 0, 0));

        registrar.register();

        ArgumentCaptor<FacadeResourceRegistrySyncCommandBO> captor =
                ArgumentCaptor.forClass(FacadeResourceRegistrySyncCommandBO.class);
        verify(facade).sync(captor.capture());
        assertThat(captor.getValue().getApis()).containsExactly(probe, write);
        assertThat(captor.getValue().isDeleteMissing()).isFalse();
    }

    @Test
    void scannerFailureWithoutFailFastIsSwallowed() {
        properties.setServiceName("svc");
        properties.setFailFast(false);
        when(scanner.scan()).thenThrow(new RuntimeException("scan boom"));

        assertThatNoException().isThrownBy(() -> registrar.register());
        verify(facade, never()).sync(any());
    }

    @Test
    void scannerFailureWithFailFastPropagates() {
        properties.setServiceName("svc");
        properties.setFailFast(true);
        when(scanner.scan()).thenThrow(new RuntimeException("scan boom"));

        assertThatThrownBy(() -> registrar.register())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("scan boom");
        verify(facade, never()).sync(any());
    }

    @Test
    void facadeFailureWithoutFailFastIsSwallowed() {
        properties.setServiceName("svc");
        properties.setFailFast(false);
        when(scanner.scan()).thenReturn(List.of());
        when(facade.sync(any())).thenThrow(new RuntimeException("downstream offline"));

        assertThatNoException().isThrownBy(() -> registrar.register());
    }

    @Test
    void facadeFailureWithFailFastPropagates() {
        properties.setServiceName("svc");
        properties.setFailFast(true);
        when(scanner.scan()).thenReturn(List.of());
        when(facade.sync(any())).thenThrow(new RuntimeException("downstream offline"));

        assertThatThrownBy(() -> registrar.register())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("downstream offline");
    }

    private static FacadeScannedApiBO api(String method, String path) {
        return FacadeScannedApiBO.builder()
                .method(method)
                .path(path)
                .apiName("Sample." + method.toLowerCase())
                .title(method.toLowerCase())
                .remark("")
                .apiGroup("Sample")
                .build();
    }

    private static FacadeResourceRegistrySyncResultBO result(int inserted, int updated, int deleted, int unchanged) {
        return FacadeResourceRegistrySyncResultBO.builder()
                .inserted(inserted)
                .updated(updated)
                .deleted(deleted)
                .unchanged(unchanged)
                .build();
    }
}
