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

package io.github.pnoker.common.mqtt.entity.property;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "driver.mqtt")
public class MqttProperties {
    @NotBlank(message = "Url can't be empty, ssl://host:port")
    private String url;

    @NotNull(message = "Auth type can't be empty")
    private AuthTypeEnum authType = AuthTypeEnum.NONE;

    private String username;
    private String password;

    private String caCrt = "classpath:/certs/ca.crt";
    private String clientKeyPass = "dc3-client";
    private String clientKey = "classpath:/certs/client.key";
    private String clientCrt = "classpath:/certs/client.crt";

    @NotBlank(message = "Client name can't be empty")
    private String client;

    @NotBlank(message = "Topic prefix name can't be empty")
    private String topicPrefix;

    @NotNull(message = "Default topic can't be empty")
    private Topic defaultSendTopic = new Topic("dc3/d/v/dc3-driver-mqtt_default", 2);

    @Size(min = 1, message = "Receive topic at least one topic")
    private List<Topic> receiveTopics;

    @NotNull(message = "Keep alive interval can't be empty")
    private Integer keepAlive = 15;

    @NotNull(message = "Completion timeout can't be empty")
    private Integer completionTimeout = 3000;


    /**
     * Mqtt 权限认证类型枚举
     */
    @NoArgsConstructor
    public enum AuthTypeEnum {
        NONE,
        CLIENT_ID,
        USERNAME,
        X509,
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Topic {
        @NotBlank(message = "Topic name can't be empty")
        private String name;

        @Min(0)
        @Max(2)
        private Integer qos;
    }

}
