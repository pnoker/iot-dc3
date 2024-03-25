package io.github.pnoker.center.data.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pnoker.center.data.service.ClusterService;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Service
public class ClusterServiceImpl implements ClusterService {
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
        StringBuilder response = new StringBuilder();
        URL url = new URL(queryUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
}
