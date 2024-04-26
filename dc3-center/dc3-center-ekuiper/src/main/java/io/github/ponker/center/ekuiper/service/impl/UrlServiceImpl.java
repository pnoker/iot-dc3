package io.github.ponker.center.ekuiper.service.impl;
import io.github.ponker.center.ekuiper.service.UrlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author : Zhen
 * @date : 2024/2/20
 */

@Slf4j
@Service
public class UrlServiceImpl implements UrlService {

    @Value("${driver.custom.STREAM_URL}")
    private String streamUrl;

    @Value("${driver.custom.TABLE_URL}")
    private String tableUrl;

    @Value("${driver.custom.CONFKEY_URL}")
    private String confKeyUrl;

    @Value("${driver.custom.RULE_URL}")
    private String ruleUrl;

    @Override
    public String getStreamUrl() {
        return streamUrl;
    }

    @Override
    public String getTableUrl() {
        return tableUrl;
    }

    @Override
    public String getConfigUrl() {
        return confKeyUrl;
    }

    @Override
    public String getRuleUrl() {
        return ruleUrl;
    }
}
