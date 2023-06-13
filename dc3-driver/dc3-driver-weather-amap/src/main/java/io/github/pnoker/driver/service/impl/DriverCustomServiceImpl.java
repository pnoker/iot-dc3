/*
 * Copyright 2016-present the original author or authors.
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

package io.github.pnoker.driver.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.common.constant.driver.EventConstant;
import io.github.pnoker.common.entity.DeviceEvent;
import io.github.pnoker.common.entity.driver.AttributeInfo;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.Device;
import io.github.pnoker.common.model.Point;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.driver.entity.WeatherInfo;
import io.github.pnoker.driver.sdk.DriverContext;
import io.github.pnoker.driver.sdk.service.DriverCustomService;
import io.github.pnoker.driver.sdk.service.DriverSenderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.github.pnoker.driver.sdk.utils.DriverUtil.attribute;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverCustomServiceImpl implements DriverCustomService {

    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverSenderService driverSenderService;
    @Value("${driver.custom.key}")
    private String key;

    @Override
    public void initial() {
        /*
        !!! 提示：此处逻辑仅供参考，请务必结合实际应用场景。!!!
        !!!
        你可以在此处执行一些特定的初始化逻辑，驱动在启动的时候会自动执行该方法。
        */
    }

    @Override
    public void schedule() {
        /*
        !!! 提示：此处逻辑仅供参考，请务必结合实际应用场景。!!!
        !!!
        上传设备状态，可自行灵活拓展，不一定非要在schedule()接口中实现，你可以：
        - 在read中实现设备状态的判断；
        - 在自定义定时任务中实现设备状态的判断；
        - 通过某种判断机制实现设备状态的判断。

        最后通过 driverSenderService.deviceStatusSender(deviceId,deviceStatus) 接口将设备状态交给SDK管理，其中设备状态（StatusEnum）：
        - ONLINE:在线
        - OFFLINE:离线
        - MAINTAIN:维护
        - FAULT:故障
         */
        driverContext.getDriverMetadata().getDeviceMap().keySet().forEach(id -> driverSenderService.deviceEventSender(new DeviceEvent(id, EventConstant.Device.STATUS, DeviceStatusEnum.ONLINE, 25, TimeUnit.SECONDS)));
    }

    @Override
    public String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) {
        /*
        !!! 提示：此处逻辑仅供参考，请务必结合实际应用场景。!!!
         */
        String city = attribute(pointInfo, "city");
        String type = attribute(pointInfo, "type");

        String response = getRequest(city);
        return getValue(type, response);
    }

    @Override
    public Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, AttributeInfo value) {
        /*
        !!! 提示：此处逻辑仅供参考，请务必结合实际应用场景。!!!
         */
        return false;
    }

    private String getRequest(String city) {
        try {
            URL url = new URL(CharSequenceUtil.format("https://restapi.amap.com/v3/weather/weatherInfo?key={}&city={}", key, city));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }

    private String getValue(String type, String request) {
        WeatherInfo weatherInfo = null;
        try {
            weatherInfo = JsonUtil.parseObject(request, WeatherInfo.class);
        } catch (Exception e) {
            return "";
        }

        if (ObjectUtil.isNull(weatherInfo) || !"10000".equals(weatherInfo.getInfoCode())) {
            return "";
        }

        WeatherInfo.Live live = weatherInfo.getLives().get(0);
        if (ObjectUtil.isNull(live)) {
            return "";
        }

        if ("weather".equals(type)) {
            return live.getWeather();
        }
        if ("temperature".equals(type)) {
            return live.getTemperature();
        }
        if ("winddirection".equals(type)) {
            return live.getWindDirection();
        }
        if ("windpower".equals(type)) {
            return live.getWindPower();
        }
        if ("humidity".equals(type)) {
            return live.getHumidity();
        }
        if ("reporttime".equals(type)) {
            return live.getReportTime();
        }

        return "";
    }

}
