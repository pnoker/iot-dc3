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
import io.github.pnoker.center.data.biz.RabbitMQMessageService;
import io.github.pnoker.center.data.entity.vo.RabbitMQDataVo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * RabbitMQMessage Service Impl
 * </p>
 *
 * @author wangshuai
 * @since 2024.3.26
 */
@Service
public class RabbitMQMessageServiceImpl implements RabbitMQMessageService {

    @Override
    public RabbitMQDataVo queryMQInMess(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery = "sum(rate(rabbitmq_global_messages_received_total[60s]) * on(instance) group_left(rabbitmq_cluster) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''})";
            return queryPromethues(promQLQuery, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RabbitMQDataVo queryMQReMess(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery = "sum(rabbitmq_queue_messages_ready * on(instance) group_left(rabbitmq_cluster) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''})";
            return queryPromethues(promQLQuery, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RabbitMQDataVo queryMQUnackMess(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery = "sum(rabbitmq_queue_messages_unacked * on(instance) group_left(rabbitmq_cluster) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''})";
            return queryPromethues(promQLQuery, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RabbitMQDataVo queryMQOutMess(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery1 = "sum(rate(rabbitmq_global_messages_redelivered_total[60s]) * on(instance) group_left(rabbitmq_cluster) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''})+";
            String promQLQuery2 = "sum(rate(rabbitmq_global_messages_delivered_consume_auto_ack_total[60s]) * on(instance) group_left(rabbitmq_cluster) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''})+";
            String promQLQuery3 = "sum(rate(rabbitmq_global_messages_delivered_consume_manual_ack_total[60s]) * on(instance) group_left(rabbitmq_cluster) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''})+";
            String promQLQuery4 = "sum(rate(rabbitmq_global_messages_delivered_get_auto_ack_total[60s]) * on(instance) group_left(rabbitmq_cluster) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''})+";
            String promQLQuery5 = "sum(rate(rabbitmq_global_messages_delivered_get_manual_ack_total[60s]) * on(instance) group_left(rabbitmq_cluster) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''})";
            String promQLQuery = promQLQuery1 + promQLQuery2 + promQLQuery3 + promQLQuery4 + promQLQuery5;
            return queryPromethues(promQLQuery, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RabbitMQDataVo queryMQMessPub(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery = "sum(rate(rabbitmq_global_messages_received_total[60s]) * on(instance) group_left(rabbitmq_cluster, rabbitmq_node) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''}) by(rabbitmq_node)";
            return queryPromethues(promQLQuery, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RabbitMQDataVo queryMQConfPub(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery = "sum(rate(rabbitmq_global_messages_confirmed_total[60s]) * on(instance) group_left(rabbitmq_cluster, rabbitmq_node) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''}) by(rabbitmq_node)";
            return queryPromethues(promQLQuery, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RabbitMQDataVo queryMQRoutQue(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery = "sum(rate(rabbitmq_global_messages_routed_total[60s]) * on(instance) group_left(rabbitmq_cluster, rabbitmq_node) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''}) by(rabbitmq_node)";
            return queryPromethues(promQLQuery, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RabbitMQDataVo queryMQUnConfPub(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery1 = "sum(rate(rabbitmq_global_messages_received_confirm_total[60s]) * on(instance) group_left(rabbitmq_cluster, rabbitmq_node) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''} -";
            String promQLQuery2 = "rate(rabbitmq_global_messages_confirmed_total[60s]) * on(instance) group_left(rabbitmq_cluster, rabbitmq_node) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''}) by(rabbitmq_node)";
            String promQLQuery = promQLQuery1 + promQLQuery2;
            return queryPromethues(promQLQuery, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RabbitMQDataVo queryMQUnRoutDrop(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery = "sum(rate(rabbitmq_global_messages_unroutable_dropped_total[60s]) * on(instance) group_left(rabbitmq_cluster, rabbitmq_node) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''}) by(rabbitmq_node)";
            return queryPromethues(promQLQuery, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RabbitMQDataVo queryMQUnRoutPub(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery = "sum(rate(rabbitmq_global_messages_unroutable_returned_total[60s]) * on(instance) group_left(rabbitmq_cluster, rabbitmq_node) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''}) by(rabbitmq_node)";
            return queryPromethues(promQLQuery, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RabbitMQDataVo queryMQMessDel(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery1 = "sum( (rate(rabbitmq_global_messages_delivered_consume_auto_ack_total[60s]) * on(instance) group_left(rabbitmq_cluster, rabbitmq_node) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''}) + ";
            String promQLQuery2 = "(rate(rabbitmq_global_messages_delivered_consume_manual_ack_total[60s]) * on(instance) group_left(rabbitmq_cluster, rabbitmq_node) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''})) by(rabbitmq_node)";
            String promQLQuery = promQLQuery1 + promQLQuery2;
            return queryPromethues(promQLQuery, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RabbitMQDataVo queryMQMessReDel(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery = "sum(rate(rabbitmq_global_messages_redelivered_total[60s]) * on(instance) group_left(rabbitmq_cluster, rabbitmq_node) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''}) by(rabbitmq_node)";
            return queryPromethues(promQLQuery, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    @Override
    public RabbitMQDataVo queryMQMessDelAck(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery = "sum(rate(rabbitmq_global_messages_delivered_consume_manual_ack_total[60s]) * on(instance) group_left(rabbitmq_cluster, rabbitmq_node) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''}) by(rabbitmq_node)";
            return queryPromethues(promQLQuery, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RabbitMQDataVo queryMQMessDelAuto(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery = "sum(rate(rabbitmq_global_messages_delivered_consume_auto_ack_total[60s]) * on(instance) group_left(rabbitmq_cluster, rabbitmq_node) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''}) by(rabbitmq_node)";
            return queryPromethues(promQLQuery, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RabbitMQDataVo queryMQMessAck(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery = "sum(rate(rabbitmq_global_messages_acknowledged_total[60s]) * on(instance) group_left(rabbitmq_cluster, rabbitmq_node) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''}) by(rabbitmq_node)";
            return queryPromethues(promQLQuery, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RabbitMQDataVo queryMQPoAutoAck(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery = "sum(rate(rabbitmq_global_messages_delivered_get_auto_ack_total[60s]) * on(instance) group_left(rabbitmq_cluster, rabbitmq_node) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''}) by(rabbitmq_node)";
            return queryPromethues(promQLQuery, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    @Override
    public RabbitMQDataVo queryMQPoNoResult(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery = "sum(rate(rabbitmq_global_messages_get_empty_total[60s]) * on(instance) group_left(rabbitmq_cluster, rabbitmq_node) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''}) by(rabbitmq_node)";
            return queryPromethues(promQLQuery, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RabbitMQDataVo queryMQPoWithAck(String cluster) {
        try {
            // 构建原始 PromQL 查询字符串
            String promQLQuery = "sum(rate(rabbitmq_global_messages_delivered_get_manual_ack_total[60s]) * on(instance) group_left(rabbitmq_cluster, rabbitmq_node) rabbitmq_identity_info{rabbitmq_cluster='" + cluster + "', namespace=''}) by(rabbitmq_node)";
            return queryPromethues(promQLQuery, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    //处理Promethues接口调用，接收数据
    private RabbitMQDataVo queryPromethues(String promQLQuery, boolean iord) throws Exception {
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
    private String sendGetRequest(String queryUrl) throws IOException {
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
}