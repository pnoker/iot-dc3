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

package io.github.pnoker.driver.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class WeatherInfo {
    /**
     * 返回状态
     */
    private String status;

    /**
     * 返回结果总数目
     */
    private String count;

    /**
     * 返回的状态信息
     */
    private String info;

    /**
     * 返回状态说明,10000代表正确
     */
    @JsonProperty("infocode")
    private String infoCode;

    /**
     * 实况天气数据信息
     */
    private List<Live> lives;

    @Data
    @NoArgsConstructor
    public static class Live {
        /**
         * 省份名
         */
        private String province;

        /**
         * 城市名
         */
        private String city;

        /**
         * 区域编码
         */
        @JsonProperty("adcode")
        private String adCode;

        /**
         * 天气现象（汉字描述）
         */
        private String weather;

        /**
         * 实时气温，单位：摄氏度
         */
        private String temperature;

        /**
         * 风向描述
         */
        @JsonProperty("winddirection")
        private String windDirection;

        /**
         * 风力级别，单位：级
         */
        @JsonProperty("windpower")
        private String windPower;

        /**
         * 空气湿度
         */
        private String humidity;

        /**
         * 数据发布的时间
         */
        @JsonProperty("reporttime")
        private String reportTime;
    }
}
