/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.driver.bean;

import com.dc3.common.valid.Insert;
import com.dc3.common.valid.Update;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @author pnoker
 */
@Setter
@Getter
@Validated({Insert.class, Update.class})
@ConfigurationProperties(prefix = "driver.mqtt")
public class MqttProperty {
    @NotBlank(message = "username can't be empty")
    private String username;

    @NotBlank(message = "password can't be empty")
    private String password;

    @NotBlank(message = "url can't be empty")
    private String url;

    @Size(min = 1, message = "at least one qos")
    private List<Integer> qos;

    @NotNull(message = "keep alive interval can't be empty")
    private Integer keepAlive;

    @NotNull(message = "completion timeout can't be empty")
    private Integer completionTimeout;

    @Size(min = 1, message = "at least one topic")
    private List<String> topics;

}
