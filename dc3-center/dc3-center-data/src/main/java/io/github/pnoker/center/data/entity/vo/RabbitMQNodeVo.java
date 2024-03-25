package io.github.pnoker.center.data.entity.vo;

import lombok.Data;

@Data
public class RabbitMQNodeVo {
    private Metric metric;
    private ValueItem value;


    @Data
    public static class Metric {
        private String erlangVersion;
        private String instance;
        private String job;
        private String prometheusClientVersion;
        private String prometheusPluginVersion;
        private String rabbitmqCluster;
        private String rabbitmqNode;
        private String rabbitmqVersion;
    }
    @Data
    public static class ValueItem {
        private String tValue;
        private String sValue;
    }
}
