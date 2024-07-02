/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.utils;

import cn.hutool.core.text.CharSequenceUtil;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.mqtt.entity.property.MqttProperties;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

/**
 * @author pnoker
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
