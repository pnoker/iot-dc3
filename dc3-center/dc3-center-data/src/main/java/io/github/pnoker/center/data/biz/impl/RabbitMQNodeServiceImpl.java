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

package io.github.pnoker.center.data.biz.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pnoker.center.data.biz.RabbitMQNodeService;
import io.github.pnoker.center.data.entity.vo.RabbitMQDataVo;
import io.github.pnoker.center.data.entity.vo.RabbitMQNodeVo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * RabbitMQNode Service Impl
 * </p>
 *
 * @author wangshuai
 * @since 2024.3.26
 */
@Service
public class RabbitMQNodeServiceImpl implements RabbitMQNodeService {
    @Override
    public RabbitMQDataVo queryNode(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery = "sum(rabbitmq_build_info * on(instance) group_left(rabbitmq_cluster) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''})";
            return queryPrometheus(promQLQuery, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<RabbitMQNodeVo> queryNodeTable(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery = "rabbitmq_build_info * on(instance) group_left(rabbitmq_cluster, rabbitmq_node) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''}";
            // 将原始查询字符串转换为 URL 编码格式
            String encodedQuery = URLEncoder.encode(promQLQuery, "UTF-8");
            // 构建查询 URL
            String queryUrl = "http://10.6.0.107:9090/api/v1/query?query=" + encodedQuery;
            String jsonResponse = sendGetRequest(queryUrl);
            // 解析 JSON 响应
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            //获取节点个数
            int n = rootNode.path("data").path("result").size();
            List<RabbitMQNodeVo> rabbitMQNodeVoList = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                JsonNode metricNode = rootNode.path("data").path("result").get(i).path("metric");
                JsonNode valueNode = rootNode.path("data").path("result").get(i).path("value");
                RabbitMQNodeVo rabbitMQNodeVo = new RabbitMQNodeVo();
                RabbitMQNodeVo.Metric metric = new RabbitMQNodeVo.Metric();
                RabbitMQNodeVo.ValueItem valueItem = new RabbitMQNodeVo.ValueItem();
                //给metric赋值
                metric.setErlangVersion(metricNode.path("erlang_version").asText());
                metric.setInstance(metricNode.path("instance").asText());
                metric.setJob(metricNode.path("job").asText());
                metric.setPrometheusClientVersion(metricNode.path("prometheus_client_version").asText());
                metric.setPrometheusPluginVersion(metricNode.path("prometheus_plugin_version").asText());
                metric.setRabbitmqCluster(metricNode.path("rabbitmq_cluster").asText());
                metric.setRabbitmqNode(metricNode.path("rabbitmq_node").asText());
                metric.setRabbitmqVersion(metricNode.path("rabbitmq_version").asText());
                rabbitMQNodeVo.setMetric(metric);
                //给value赋值
                valueItem.setTValue(UnTimeUnix(valueNode.get(0).asDouble()));
                valueItem.setSValue(valueNode.get(1).asText());
                rabbitMQNodeVo.setValue(valueItem);
                rabbitMQNodeVoList.add(rabbitMQNodeVo);
            }
            return rabbitMQNodeVoList;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //处理Prometheus接口调用，接收数据
    private RabbitMQDataVo queryPrometheus(String promQLQuery, boolean iord) throws Exception {
        // 将原始查询字符串转换为 URL 编码格式
        String encodedQuery = URLEncoder.encode(promQLQuery, "UTF-8");
        // 获取当前时间并转换为 UTC 时间
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        // 计算前 15 分钟之前的时间并转换为 UTC 时间
        LocalDateTime fifteenMinutesAgo = now.minusMinutes(15);
        long time1 = fifteenMinutesAgo.toEpochSecond(ZoneOffset.UTC);
        long time2 = now.toEpochSecond(ZoneOffset.UTC);
        // 构建查询 URL
        String queryUrl = "http://10.6.0.107:9090/api/v1/query_range?query=" + encodedQuery;
        String jsonResponse = sendGetRequest(queryUrl+"&start="+time1+"&end="+time2+"&step=15");
        // 解析 JSON 响应
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        JsonNode resultNode = rootNode.path("data").path("result").get(0);
        List<Double> values = new ArrayList<>();
        List<Integer> ivalues = new ArrayList<>();
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < 61; i++) {
            long time = resultNode.path("values").get(i).get(0).asLong();
            times.add(time);
            if (iord == true) {//根据iord判断给前端的值时double类型，还是int类型
                double value = resultNode.path("values").get(i).get(1).asDouble();
                values.add(value);
            } else {
                int ivalue = resultNode.path("values").get(i).get(1).asInt();
                ivalues.add(ivalue);
            }
        }
        RabbitMQDataVo rabbitMQDataVo = new RabbitMQDataVo();
        rabbitMQDataVo.setTimes(times);
        rabbitMQDataVo.setValues(values);
        rabbitMQDataVo.setIvalues(ivalues);
        return rabbitMQDataVo;
    }

    // 发送 HTTP GET 请求并返回响应内容
    private static String sendGetRequest(String queryUrl) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(60, TimeUnit.SECONDS) // 设置调用超时时间为60秒
                .connectTimeout(60, TimeUnit.SECONDS) // 设置连接超时时间为60秒
                .readTimeout(60, TimeUnit.SECONDS) // 设置读取超时时间为60秒
                .build();
        Request request = new Request.Builder()
                .url(queryUrl)
                .get()
                .build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Request failed or empty response");
        }
        return response.body().string();
    }

    public static String UnTimeUnix(Double dtime) {
        // 将 UNIX 时间戳转换为 Instant 对象
        Instant instant = Instant.ofEpochSecond(Math.round(dtime));// 2024-03-18 08:04:00
        // 将 Instant 对象转换为 LocalDateTime 对象，使用默认时区
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        // 定义日期时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 格式化日期和时间
        String formattedDateTime = dateTime.format(formatter);
        return formattedDateTime;
    }
}