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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

/**
 * @author : Zhen
 */

@Slf4j
@RestController
@RequestMapping(ServiceConstant.Ekuiper.SINKTEM_URL_PREFIX)
public class SinkTemplateController {

    @Resource
    private ApiService apiService;

    @Resource
    private ConfigService configService;

    @Resource
    private SinkTemplateService sinkTemplateService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private UrlService urlService;

    /**
     *注册、更新SinkTemplate
     */
    @PutMapping("/create")
    public Mono<R<String>> createSink(@Valid @RequestBody Object form, @RequestParam(name = "name") String name, @RequestParam(name = "confKey") String confKey) {
        String url = urlService.getConfigUrl() + "/sinks/" + name + "/confKeys/" + confKey;
        Mono<String> stringMono = sinkTemplateService.callApiSinktem(HttpMethod.PUT, url, form, name);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to call createSink API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     *列出{name}类型的全部SinkTemplate
     */
    @GetMapping("/list")
    public Mono<R<Map<String, Map<String, Object>>>> listSink(@RequestParam(name = "name") String name) {
        String url = urlService.getConfigUrl() + "/sinks/yaml/" + name;
        Mono<String> stringMono = apiService.callApi(HttpMethod.GET, url);
        return stringMono.flatMap(jsonString -> {
            try {
                TypeReference<Map<String, Map<String, Object>>> typeRef = new TypeReference<Map<String, Map<String, Object>>>(){};
                Map<String, Map<String, Object>> configurations = objectMapper.readValue(jsonString, typeRef);
                return Mono.just(R.ok(configurations,"successful"));
            } catch (Exception e) {
                log.error("Failed to call listSink API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     *删除SinkTemplate
     */
    @DeleteMapping("/delete")
    public Mono<R<String>> deleteSink(@RequestParam(name = "name") String name, @RequestParam(name = "confKey") String confKey) {
        String url = urlService.getConfigUrl() + "/sinks/" + name + "/confKeys/" + confKey;
        Mono<String> stringMono = configService.callApiDelConf(HttpMethod.DELETE, url);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to call deleteSink API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     *测试SinkTemplate连接
     */
    @PostMapping("/connection")
    public Mono<R<String>> connectSink(@Valid @RequestBody Object form, @RequestParam(name = "name") String name){
        String url = urlService.getConfigUrl() + "/sinks/connection/" + name ;
        Mono<String> stringMono = sinkTemplateService.callApiSinktem(HttpMethod.POST,url,form,name);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to call connectSink API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }
}
