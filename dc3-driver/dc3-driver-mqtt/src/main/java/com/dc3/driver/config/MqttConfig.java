/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.driver.config;

import cn.hutool.core.util.StrUtil;
import com.dc3.common.constant.Common;
import com.dc3.common.exception.ServiceException;
import com.dc3.driver.bean.MqttProperty;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import javax.annotation.Resource;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;


/**
 * @author pnoker
 */
@Slf4j
@Configuration
@IntegrationComponentScan
@EnableConfigurationProperties({MqttProperty.class})
public class MqttConfig {
    @Value("${driver.mqtt.client.id}")
    private String clientId;

    @Value("${driver.mqtt.default.topic}")
    private String defaultTopic;

    @Value("${driver.mqtt.default.qos}")
    private Integer defaultQos;

    @Resource
    private MqttProperty mqttProperty;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(getMqttConnectOptions());
        return factory;
    }

    @Bean
    public MqttConnectOptions getMqttConnectOptions() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();

        // username & password
        if (mqttProperty.getAuthType().equals(MqttProperty.AuthTypeEnum.USERNAME)) {
            mqttConnectOptions.setUserName(mqttProperty.getUsername());
            mqttConnectOptions.setPassword(mqttProperty.getPassword().toCharArray());
        }

        // tls x509
        if (mqttProperty.getAuthType().equals(MqttProperty.AuthTypeEnum.X509)) {
            mqttConnectOptions.setSocketFactory(getSocketFactory(
                    mqttProperty.getCaCrt(),
                    mqttProperty.getClientCrt(),
                    mqttProperty.getClientKey(),
                    StrUtil.isBlank(mqttProperty.getClientKeyPass()) ? "" : mqttProperty.getClientKeyPass()
            ));
            if (!StrUtil.isBlank(mqttProperty.getUsername()) && !StrUtil.isBlank(mqttProperty.getPassword())) {
                mqttConnectOptions.setUserName(mqttProperty.getUsername());
                mqttConnectOptions.setPassword(mqttProperty.getPassword().toCharArray());
            }
        }

        mqttConnectOptions.setServerURIs(new String[]{mqttProperty.getUrl()});
        mqttConnectOptions.setKeepAliveInterval(mqttProperty.getKeepAlive());
        return mqttConnectOptions;
    }

    @Bean
    public MessageChannel defaultMqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * 说明：
     * ConditionalOnProperty(value = "driver.mqtt.default.receive.enable")
     * 根据配置属性driver.mqtt.default.receive.enable选择是否开启 Default Topic 主题的数据接收逻辑
     *
     * @return
     */
    @Bean
    @ConditionalOnProperty(value = "driver.mqtt.default.receive.enable")
    public MessageProducer defaultInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                clientId + "_default_inbound_",
                mqttClientFactory(),
                defaultTopic
        );
        adapter.setCompletionTimeout(mqttProperty.getCompletionTimeout());
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(defaultQos);
        adapter.setOutputChannel(defaultMqttInputChannel());
        return adapter;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttOutputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer inbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                clientId + "_inbound",
                mqttClientFactory(),
                mqttProperty.getTopics().toArray(new String[0])
        );
        adapter.setCompletionTimeout(mqttProperty.getCompletionTimeout());
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(mqttProperty.getQos().stream().mapToInt(Integer::valueOf).toArray());
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutputChannel")
    public MessageHandler outbound() {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(clientId, mqttClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setDefaultQos(defaultQos);
        messageHandler.setDefaultTopic(defaultTopic);
        return messageHandler;
    }

    private SSLSocketFactory getSocketFactory(final String caCrtFile, final String crtFile, final String keyFile, final String password) {
        try {
            Security.addProvider(new BouncyCastleProvider());

            PEMReader reader = null;
            // load CA certificate
            if (caCrtFile.startsWith(Common.CLASSPATH)) {
                reader = new PEMReader(new InputStreamReader(this.getClass().getResourceAsStream(caCrtFile.replace(Common.CLASSPATH, ""))));
            } else {
                reader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(caCrtFile)))));
            }
            X509Certificate caCert = (X509Certificate) reader.readObject();
            reader.close();

            // load client certificate
            if (crtFile.startsWith(Common.CLASSPATH)) {
                reader = new PEMReader(new InputStreamReader(this.getClass().getResourceAsStream(crtFile.replace(Common.CLASSPATH, ""))));
            } else {
                reader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(crtFile)))));
            }
            X509Certificate cert = (X509Certificate) reader.readObject();
            reader.close();

            // load client private key
            if (keyFile.startsWith(Common.CLASSPATH)) {
                reader = new PEMReader(new InputStreamReader(this.getClass().getResourceAsStream(keyFile.replace(Common.CLASSPATH, ""))), password::toCharArray);
            } else {
                reader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(keyFile)))), password::toCharArray);
            }
            KeyPair key = (KeyPair) reader.readObject();
            reader.close();

            // CA certificate is used to authenticate server
            KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
            caKs.load(null, null);
            caKs.setCertificateEntry("cacertfile", caCert);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(caKs);

            // client key and certificates are sent to server so it can authenticate us
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            ks.setCertificateEntry("certfile", cert);
            ks.setKeyEntry("keyfile", key.getPrivate(), password.toCharArray(), new java.security.cert.Certificate[]{cert});
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, password.toCharArray());

            // finally, create SSL socket factory
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            return context.getSocketFactory();
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

}