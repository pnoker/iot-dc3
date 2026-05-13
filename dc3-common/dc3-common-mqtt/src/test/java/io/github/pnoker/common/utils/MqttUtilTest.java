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

package io.github.pnoker.common.utils;

import io.github.pnoker.common.mqtt.entity.property.MqttProperties;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MqttUtilTest {

    private static MqttProperties baseProperties() {
        MqttProperties properties = new MqttProperties();
        properties.setUrl("tcp://broker.example.com:1883");
        properties.setKeepAlive(30);
        return properties;
    }

    @Test
    void noneAuthLeavesCredentialsUnset() {
        MqttProperties properties = baseProperties();
        properties.setAuthType(MqttProperties.AuthTypeEnum.NONE);
        properties.setUsername("not-applied");
        properties.setPassword("not-applied");

        MqttConnectOptions options = MqttUtil.getMqttConnectOptions(properties);
        assertThat(options.getUserName()).isNull();
        assertThat(options.getPassword()).isNull();
        assertThat(options.getServerURIs()).containsExactly("tcp://broker.example.com:1883");
        assertThat(options.getKeepAliveInterval()).isEqualTo(30);
    }

    @Test
    void usernameAuthAppliesCredentialsToOptions() {
        MqttProperties properties = baseProperties();
        properties.setAuthType(MqttProperties.AuthTypeEnum.USERNAME);
        properties.setUsername("alice");
        properties.setPassword("secret");

        MqttConnectOptions options = MqttUtil.getMqttConnectOptions(properties);
        assertThat(options.getUserName()).isEqualTo("alice");
        assertThat(options.getPassword()).containsExactly('s', 'e', 'c', 'r', 'e', 't');
    }

    @Test
    void usernameAuthSilentlySkipsBlankUsername() {
        MqttProperties properties = baseProperties();
        properties.setAuthType(MqttProperties.AuthTypeEnum.USERNAME);
        properties.setUsername("");
        properties.setPassword("secret");
        MqttConnectOptions options = MqttUtil.getMqttConnectOptions(properties);
        assertThat(options.getUserName()).isNull();
        assertThat(options.getPassword()).containsExactly('s', 'e', 'c', 'r', 'e', 't');
    }

    @Test
    void usernameAuthSilentlySkipsBlankPassword() {
        MqttProperties properties = baseProperties();
        properties.setAuthType(MqttProperties.AuthTypeEnum.USERNAME);
        properties.setUsername("alice");
        properties.setPassword("");
        MqttConnectOptions options = MqttUtil.getMqttConnectOptions(properties);
        assertThat(options.getUserName()).isEqualTo("alice");
        assertThat(options.getPassword()).isNull();
    }

    @Test
    void httpsHostnameVerificationIsAlwaysDisabled() {
        // Verifying the platform's deliberate choice — local TLS termination at gateway,
        // hostname verification done elsewhere; broker conn uses pinned certs only.
        MqttProperties properties = baseProperties();
        properties.setAuthType(MqttProperties.AuthTypeEnum.NONE);
        MqttConnectOptions options = MqttUtil.getMqttConnectOptions(properties);
        assertThat(options.isHttpsHostnameVerificationEnabled()).isFalse();
    }

    @Test
    void utilityClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<MqttUtil> constructor = MqttUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
