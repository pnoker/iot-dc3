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

import cn.hutool.core.text.CharSequenceUtil;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.mqtt.entity.property.MqttProperties;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

/**
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class MqttUtil {

    private MqttUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    public static MqttConnectOptions getMqttConnectOptions(MqttProperties mqttProperties) {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();

        // username & password
        if (MqttProperties.AuthTypeEnum.USERNAME.equals(mqttProperties.getAuthType()) || MqttProperties.AuthTypeEnum.X509.equals(mqttProperties.getAuthType())) {
            if (CharSequenceUtil.isNotEmpty(mqttProperties.getUsername())) {
                mqttConnectOptions.setUserName(mqttProperties.getUsername());
            }
            if (CharSequenceUtil.isNotEmpty(mqttProperties.getPassword())) {
                mqttConnectOptions.setPassword(mqttProperties.getPassword().toCharArray());
            }
        }

        // tls x509
        if (MqttProperties.AuthTypeEnum.X509.equals(mqttProperties.getAuthType())) {
            mqttConnectOptions.setSocketFactory(X509Util.getSSLSocketFactory(
                    mqttProperties.getCaCrt(),
                    mqttProperties.getClientCrt(),
                    mqttProperties.getClientKey(),
                    CharSequenceUtil.isEmpty(mqttProperties.getClientKeyPass()) ? CharSequenceUtil.EMPTY : mqttProperties.getClientKeyPass()
            ));

        }

        // disable https hostname verification
        mqttConnectOptions.setHttpsHostnameVerificationEnabled(false);
        mqttConnectOptions.setServerURIs(new String[]{mqttProperties.getUrl()});
        mqttConnectOptions.setKeepAliveInterval(mqttProperties.getKeepAlive());
        return mqttConnectOptions;

    }
}
