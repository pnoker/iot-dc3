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
import io.github.pnoker.center.data.biz.RabbitMQClusterService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * RabbitMQCluster Service Impl
 * </p>
 *
 * @author wangshuai
 * @since 2024.3.26
 */
@Service
public class RabbitMQClusterServiceImpl implements RabbitMQClusterService {
    @Override
    public List<String> queryCluster() {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery = "rabbitmq_identity_info{namespace=''}";
            // 将原始查询字符串转换为 URL 编码格式
            String encodedQuery = URLEncoder.encode(promQLQuery, "UTF-8");
            // 构建查询 URL
            String queryUrl = "http://10.6.0.107:9090/api/v1/query?query=" + encodedQuery;
            String jsonResponse = sendGetRequest(queryUrl);
            // 解析 JSON 响应
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            int n = rootNode.path("data").path("result").size();
            List<String> clusters = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                JsonNode metricNode = rootNode.path("data").path("result").get(i).path("metric");
                String str = metricNode.path("rabbitmq_cluster").asText();
                clusters.add(str);
            }
            return clusters;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String sendGetRequest(String queryUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
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
}
