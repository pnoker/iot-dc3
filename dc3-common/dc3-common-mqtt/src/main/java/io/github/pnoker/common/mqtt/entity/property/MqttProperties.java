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
 * @version 2025.6.0
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
