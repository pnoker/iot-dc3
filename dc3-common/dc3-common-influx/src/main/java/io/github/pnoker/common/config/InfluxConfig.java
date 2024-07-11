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

package io.github.pnoker.common.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.domain.IsOnboarding;
import com.influxdb.client.domain.OnboardingRequest;
import com.influxdb.client.domain.OnboardingResponse;
import com.influxdb.client.service.SetupService;
import io.github.pnoker.common.influx.entity.property.InfluxProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Response;

import java.io.IOException;
import java.time.Instant;

/**
 * InfluxDB config
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Configuration
public class InfluxConfig {

    private final InfluxProperties properties;

    public InfluxConfig(InfluxProperties properties) {
        this.properties = properties;
    }

    /**
     * 返回influx http客户端,如果用代码的方式进行初始化,则务必查看日志保留token或设置密码
     *
     * @return InfluxDBClient
     * @throws IOException IOException
     */
    @Bean
    public InfluxDBClient influxDBClient() throws IOException {
        //todo 这里没有校验token和user同时为null的情况
        if (properties.getToken() == null || properties.getToken().isEmpty()) {
            //check step
            InfluxDBClient influxDBClient = InfluxDBClientFactory.create(properties.getUrl());
            SetupService setupService = influxDBClient.getService(SetupService.class);
            String zapTraceSpan = Instant.now().toString();
            Response<IsOnboarding> response = setupService.getSetup(zapTraceSpan).execute();
            if (response.isSuccessful() && response.body() != null && response.body().getAllowed()) {
                //未初始化
                OnboardingRequest onboardingRequest = new OnboardingRequest();
                //必选
                onboardingRequest.setUsername(properties.getUsername());
                onboardingRequest.setOrg(properties.getOrganization());
                onboardingRequest.setBucket(properties.getBucket());
                //可选 如果不设置密码则无法登录web ui
                onboardingRequest.setPassword(properties.getPassword());
                Response<OnboardingResponse> onBoardingResponse = setupService.postSetup(onboardingRequest, zapTraceSpan).execute();
                OnboardingResponse onboardingResponse = onBoardingResponse.body();
                log.info("influx setup:token{}", onboardingResponse.getAuth().getToken());
                return InfluxDBClientFactory.create(properties.getUrl(), onboardingResponse.getAuth().getToken().toCharArray(), properties.getOrganization(), properties.getBucket());
            }
        }
        return InfluxDBClientFactory.create(properties.getUrl(), properties.getToken().toCharArray(), properties.getOrganization(), properties.getBucket());
    }

}
