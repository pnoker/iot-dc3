package io.github.ponker.center.ekuiper.constant;

/**
 * @author : Zhen
 * <p>
 * 规则引擎相关
 */
public interface EkuiperConstant {

    /**
     * 规则引擎url相关
     */
    interface EKUIPERURL {

        String STREAM_URL = "http://localhost:9081/streams";
        String TABLE_URL = "http://localhost:9081/tables";
        String CONFKEY_URL = "http://localhost:9081/metadata/sources";
        String RULE_URL = "http://localhost:9081/rules";
    }


    interface ConKeyType {

        String MQTT = "mqtt";
        String REDIS = "redis";
        String HTTPPULL = "httppull";
        String HTTPPUSH = "httppush";
        String SQL = "sql";
        String REST = "rest";
    }

}
