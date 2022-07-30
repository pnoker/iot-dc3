/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.common.sdk.bean.mqtt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.*;
import java.util.List;

/**
 * @author pnoker
 */
@Data
@Validated
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "driver.mqtt")
public class MqttProperties {
    @NotBlank(message = "url can't be empty，ssl://host:port")
    private String url;

    @NotNull(message = "auth type can't be empty")
    private AuthTypeEnum authType = AuthTypeEnum.NONE;

    private String username;
    private String password;

    private String caCrt = "classpath:/certs/ca.crt";
    private String clientKeyPass = "dc3-client";
    private String clientKey = "classpath:/certs/client.key";
    private String clientCrt = "classpath:/certs/client.crt";

    @NotBlank(message = "client name can't be empty")
    private String client;

    @NotNull(message = "default topic can't be empty")
    private Topic defaultSendTopic = new Topic("dc3/d/v/dc3-driver-mqtt_default", 2);

    @Size(min = 1, message = "receive topic at least one topic")
    private List<Topic> receiveTopics;

    @NotNull(message = "keep alive interval can't be empty")
    private Integer keepAlive = 15;

    @NotNull(message = "completion timeout can't be empty")
    private Integer completionTimeout = 3000;


    /**
     * Mqtt 权限认证类型枚举
     */
    @NoArgsConstructor
    public enum AuthTypeEnum {
        NONE, CLIENT_ID, USERNAME, X509
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Topic {
        @NotBlank(message = "topic name can't be empty")
        private String name;

        @Min(0)
        @Max(2)
        private Integer qos;
    }

}
