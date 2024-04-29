package io.github.ponker.center.ekuiper.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ponker.center.ekuiper.constant.ServiceConstant;
import io.github.ponker.center.ekuiper.entity.R;
import io.github.ponker.center.ekuiper.service.ApiService;
import io.github.ponker.center.ekuiper.service.ConfigService;
import io.github.ponker.center.ekuiper.service.SinkTemplateService;
import io.github.ponker.center.ekuiper.service.UrlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @author : Zhen
 */

@Slf4j
@RestController
@RequestMapping(ServiceConstant.Ekuiper.CONFKEY_URL_PREFIX)
public class ConKeyController {

    @Resource
    private ApiService apiService;

    @Resource
    private ConfigService configService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private UrlService urlService;

    @Resource
    private SinkTemplateService sinkTemplateService;

    /**
     * 注册、更新ConfKey
     */
    @PutMapping("/create")
    public Mono<R<String>> createConf(@Validated @RequestBody Object form, @RequestParam(name = "name") String name, @RequestParam(name = "confKey") String confKey) {
        String url = urlService.getConfigUrl() + "/sources/" + name + "/confKeys/" + confKey;
        Mono<String> stringMono = configService.callApiConfig(HttpMethod.PUT, url, form, name);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to call create ConfKey API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * 列出{name}类型的全部Confkey
     */
    @GetMapping("/list")
    public Mono<R<Map<String, Map<String, Object>>>> listConf(@RequestParam(name = "name") String name) {
        String url = urlService.getConfigUrl() + "/sources/yaml/" + name;
        Mono<String> stringMono = apiService.callApi(HttpMethod.GET, url);
        return stringMono.flatMap(jsonString -> {
            try {
                TypeReference<Map<String, Map<String, Object>>> typeRef = new TypeReference<Map<String, Map<String, Object>>>() {
                };
                Map<String, Map<String, Object>> configurations = objectMapper.readValue(jsonString, typeRef);
                return Mono.just(R.ok(configurations, "successful"));
            } catch (Exception e) {
                log.error("Failed to call list ConfKey API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * 删除ConfKey
     */
    @DeleteMapping("/delete")
    public Mono<R<String>> deleteConf(@RequestParam(name = "name") String name, @RequestParam(name = "confKey") String confKey) {
        String url = urlService.getConfigUrl() + "/sources/" + name + "/confKeys/" + confKey;
        Mono<String> stringMono = configService.callApiDelConf(HttpMethod.DELETE, url);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to call delete ConfKey API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * 测试配置连接
     */
    @PostMapping("/connection")
    public Mono<R<String>> connectConfig(@Validated @RequestBody Object form, @RequestParam(name = "name") String name) {
        String url = urlService.getConfigUrl() + "/sources/connection/" + name;
        Mono<String> stringMono = configService.callApiConfig(HttpMethod.POST, url, form, name);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to call connect ConfKey API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * 显示所有Config和Sinktemplate
     */
    @GetMapping("/resources")
    public Mono<R<Map<String, List<Map<String, String>>>>> listAllConfig() {
        String url = urlService.getConfigUrl() + "/resources";
        Mono<String> stringMono = sinkTemplateService.callConfigAndSink(HttpMethod.GET, url);
        return stringMono.flatMap(jsonString -> {
            try {
                TypeReference<Map<String, List<Map<String, String>>>> typeRef = new TypeReference<Map<String, List<Map<String, String>>>>() {
                };
                Map<String, List<Map<String, String>>> configurations = objectMapper.readValue(jsonString, typeRef);
                return Mono.just(R.ok(configurations, "successful"));
            } catch (Exception e) {
                log.error("Failed to call list All ConfKey API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }
}
