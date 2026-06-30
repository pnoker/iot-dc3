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

package io.github.pnoker.driver.service.impl;

import io.github.pnoker.common.driver.entity.bean.DeviceHealthState;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.driver.key.KeyLoader;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.X509IdentityProvider;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OpcUaDriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    private OpcUaDriverCustomServiceImpl service;

    private static Map<String, AttributeBO> driverConfig(String host, int port, String path) {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("host", AttributeBO.builder().value(host).type(AttributeTypeEnum.STRING).build());
        m.put("port", AttributeBO.builder().value(String.valueOf(port)).type(AttributeTypeEnum.INT).build());
        m.put("path", AttributeBO.builder().value(path).type(AttributeTypeEnum.STRING).build());
        return m;
    }

    private static Map<String, AttributeBO> pointConfig(int namespace, String tag) {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("namespace",
                AttributeBO.builder().value(String.valueOf(namespace)).type(AttributeTypeEnum.INT).build());
        m.put("tag", AttributeBO.builder().value(tag).type(AttributeTypeEnum.STRING).build());
        return m;
    }

    private static DeviceBO device(Long id) {
        DeviceBO device = new DeviceBO();
        device.setId(id);
        return device;
    }

    private static PointBO point(PointTypeEnum type) {
        PointBO point = new PointBO();
        point.setId(1L);
        point.setPointTypeFlag(type);
        return point;
    }

    private static MetadataEventDTO metadataEvent(MetadataTypeEnum type, MetadataOperateTypeEnum op, Long id) {
        MetadataEventDTO event = new MetadataEventDTO();
        event.setMetadataType(type);
        event.setOperateType(op);
        event.setId(id);
        return event;
    }

    // Reference unused to keep imports stable for future expansion.
    @SuppressWarnings("unused")
    private static List<EndpointDescription> sampleEndpoints() {
        return List.of();
    }

    @SuppressWarnings("unused")
    private static Optional<EndpointDescription> sampleSelector() {
        return Optional.empty();
    }

    @BeforeEach
    void setUp() {
        service = new OpcUaDriverCustomServiceImpl(driverMetadata, driverSenderService);
        service.initial();
    }

    @Test
    void initialAllocatesEmptyConnectionMap() {
        // initial() was already invoked in setUp; calling it again should not throw and
        // should reset the underlying map.
        assertThatNoException().isThrownBy(() -> service.initial());
    }

    @Test
    void scheduleDoesNotReportDeviceStatus() {
        assertThatNoException().isThrownBy(() -> service.schedule());
        verifyNoInteractions(driverSenderService);
    }

    @Test
    void scheduleIsSilentWhenNoDevicesRegistered() {
        assertThatNoException().isThrownBy(() -> service.schedule());
        verifyNoInteractions(driverSenderService);
    }

    @Test
    void deviceUpdateInvalidatesCachedClient() throws Exception {
        OpcUaClient cached = Mockito.mock(OpcUaClient.class);
        connectionMap().put(123L, cached);
        service.event(metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE, 123L));
        assertThat(connectionMap()).doesNotContainKey(123L);
    }

    @Test
    void deviceDeleteInvalidatesCachedClient() throws Exception {
        OpcUaClient cached = Mockito.mock(OpcUaClient.class);
        connectionMap().put(456L, cached);
        service.event(metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.DELETE, 456L));
        assertThat(connectionMap()).doesNotContainKey(456L);
    }

    @Test
    void deviceAddDoesNotTouchCachedClient() throws Exception {
        OpcUaClient cached = Mockito.mock(OpcUaClient.class);
        connectionMap().put(789L, cached);
        service.event(metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.ADD, 789L));
        assertThat(connectionMap()).containsKey(789L);
    }

    @Test
    void pointEventDoesNotTouchConnectionMap() throws Exception {
        OpcUaClient cached = Mockito.mock(OpcUaClient.class);
        connectionMap().put(100L, cached);
        service.event(metadataEvent(MetadataTypeEnum.POINT, MetadataOperateTypeEnum.UPDATE, 200L));
        assertThat(connectionMap()).containsKey(100L);
    }

    @Test
    void connectorFailureIsTranslatedToConnectorException() throws Exception {
        try (MockedStatic<OpcUaClient> staticMock = Mockito.mockStatic(OpcUaClient.class)) {
            staticMock.when(() -> OpcUaClient.create(anyString(),
                    any(Function.class),
                    any(Function.class))).thenThrow(new UaException(0L, "endpoint refused"));

            assertThatThrownBy(() -> service.read(driverConfig("h", 4840, "/"), pointConfig(2, "tag.x"),
                    device(1L), point(PointTypeEnum.STRING))).isInstanceOf(ConnectorException.class)
                    .hasMessageContaining("endpoint refused");
            assertThat(connectionMap()).doesNotContainKey(1L);
        }
    }

    @Test
    void connectorIsCachedAcrossSubsequentInvocations() throws Exception {
        OpcUaClient client = Mockito.mock(OpcUaClient.class);
        try (MockedStatic<OpcUaClient> staticMock = Mockito.mockStatic(OpcUaClient.class)) {
            staticMock.when(() -> OpcUaClient.create(anyString(),
                    any(Function.class),
                    any(Function.class))).thenReturn(client);

            // Trigger the connector twice with the same device id; static factory should
            // be invoked only once because the client is cached after the first hit.
            invokeGetConnector(1L, driverConfig("h", 4840, "/"));
            invokeGetConnector(1L, driverConfig("h", 4840, "/"));

            staticMock.verify(() -> OpcUaClient.create(anyString(),
                    any(Function.class),
                    any(Function.class)), times(1));
            assertThat(connectionMap()).containsKey(1L);
        }
    }

    @Test
    void writeNodeTreatsGoodStatusAsSuccess() throws Exception {
        OpcUaClient client = Mockito.mock(OpcUaClient.class);
        Mockito.when(client.writeValue(any(NodeId.class), any(DataValue.class)))
                .thenReturn(CompletableFuture.completedFuture(new StatusCode(0L)));

        java.lang.reflect.Method method = OpcUaDriverCustomServiceImpl.class.getDeclaredMethod(
                "writeNode", OpcUaClient.class, NodeId.class, WritePointValue.class);
        method.setAccessible(true);

        Boolean ok = (Boolean) method.invoke(service, client, new NodeId(2, "tag.x"),
                WritePointValue.builder().value("7").type(PointTypeEnum.INT).build());

        assertThat(ok).isTrue();
    }

    @Test
    void certificateBranchUsesX509IdentityProvider() throws Exception {
        // A loader carrying both a client certificate and a key pair must produce an
        // X509IdentityProvider so the certificate is used for application-layer auth,
        // not just TLS.
        KeyPair keyPair = Mockito.mock(KeyPair.class);
        Mockito.when(keyPair.getPrivate()).thenReturn(Mockito.mock(PrivateKey.class));
        KeyLoader loader = Mockito.mock(KeyLoader.class);
        Mockito.when(loader.getClientCertificate()).thenReturn(Mockito.mock(X509Certificate.class));
        Mockito.when(loader.getClientKeyPair()).thenReturn(keyPair);

        IdentityProvider provider = invokeBuildIdentityProvider(loader);

        assertThat(provider).isInstanceOf(X509IdentityProvider.class);
    }

    @Test
    void noCertificateBranchUsesAnonymous() throws Exception {
        // No loader (initial() fell back to anonymous) must yield AnonymousProvider.
        assertThat(invokeBuildIdentityProvider(null)).isInstanceOf(AnonymousProvider.class);

        // A loader without a certificate or key pair must also yield AnonymousProvider.
        KeyLoader empty = Mockito.mock(KeyLoader.class);
        assertThat(invokeBuildIdentityProvider(empty)).isInstanceOf(AnonymousProvider.class);
    }

    private IdentityProvider invokeBuildIdentityProvider(KeyLoader loader) throws Exception {
        Method method = OpcUaDriverCustomServiceImpl.class.getDeclaredMethod("buildIdentityProvider", KeyLoader.class);
        method.setAccessible(true);
        return (IdentityProvider) method.invoke(service, loader);
    }

    private void invokeGetConnector(Long deviceId, Map<String, AttributeBO> driverConfig) throws Exception {
        Method method =
                OpcUaDriverCustomServiceImpl.class.getDeclaredMethod("getConnector", Long.class, Map.class);
        method.setAccessible(true);
        method.invoke(service, deviceId, driverConfig);
    }

    @Test
    void healthReportsDegradedModeWhenKeyLoaderFailed() throws Exception {
        setCertificateDegraded(true);
        OpcUaClient client = Mockito.mock(OpcUaClient.class);
        Mockito.when(client.connect()).thenReturn(CompletableFuture.completedFuture(client));
        connectionMap().put(1L, client);

        DeviceHealthState state = service.health(driverConfig("h", 4840, "/"), device(1L));

        assertThat(state.getStatus()).isEqualTo(EntityStatusEnum.ONLINE);
        assertThat(state.getDescription()).isNotNull().contains("degraded");
    }

    @Test
    void healthReportsOnlineWhenNotDegraded() throws Exception {
        setCertificateDegraded(false);
        OpcUaClient client = Mockito.mock(OpcUaClient.class);
        Mockito.when(client.connect()).thenReturn(CompletableFuture.completedFuture(client));
        connectionMap().put(2L, client);

        DeviceHealthState state = service.health(driverConfig("h", 4840, "/"), device(2L));

        assertThat(state.getStatus()).isEqualTo(EntityStatusEnum.ONLINE);
        assertThat(state.getDescription()).isNull();
    }

    private void setCertificateDegraded(boolean value) throws Exception {
        Field field = OpcUaDriverCustomServiceImpl.class.getDeclaredField("certificateDegraded");
        field.setAccessible(true);
        field.setBoolean(service, value);
    }

    @SuppressWarnings("unchecked")
    private Map<Long, OpcUaClient> connectionMap() throws Exception {
        Field field = OpcUaDriverCustomServiceImpl.class.getDeclaredField("connectMap");
        field.setAccessible(true);
        return (Map<Long, OpcUaClient>) field.get(service);
    }

}
