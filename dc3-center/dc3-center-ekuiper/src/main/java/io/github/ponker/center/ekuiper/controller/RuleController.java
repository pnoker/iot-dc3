package io.github.ponker.center.ekuiper.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.ponker.center.ekuiper.constant.ServiceConstant;
import io.github.ponker.center.ekuiper.entity.R;
import io.github.ponker.center.ekuiper.entity.vo.DetailRuleVO;
import io.github.ponker.center.ekuiper.entity.vo.RuleDataVO;
import io.github.ponker.center.ekuiper.entity.vo.RuleStatusVO;
import io.github.ponker.center.ekuiper.service.ApiService;
import io.github.ponker.center.ekuiper.service.RuleService;
import io.github.ponker.center.ekuiper.service.UrlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * @author : Zhen
 */

@Slf4j
@RestController
@RequestMapping(ServiceConstant.Ekuiper.RULE_URL_PREFIX)
public class RuleController {

    @Resource
    private ApiService apiService;

    @Resource
    private RuleService ruleService;

    @Resource
    private UrlService urlService;

    /**
     *新增rule
     */
    @PostMapping("/create")
    public Mono<R<String>> createRule(@Valid @RequestBody Object form) throws Exception {
        Mono<String> stringMono = ruleService.callRuleApiWithData(form, HttpMethod.POST, urlService.getRuleUrl());
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to call create Rule API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     *更新rule
     */
    @PutMapping("/update")
    public Mono<R<String>> updateRule(@Valid @RequestBody Object form, @RequestParam(name = "id") String id) throws Exception {
        String url = urlService.getRuleUrl() + "/" + id;
        Mono<String> stringMono = ruleService.callRuleApiWithData(form, HttpMethod.PUT, url);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to call update Rule API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     *展示所有规则
     */
    @GetMapping("/list")
    public Mono<R<Page<RuleDataVO>>> listAllRule(@RequestParam(required = false,defaultValue = "1") Integer pageNum,
                                                 @RequestParam(required = false,defaultValue = "10") Integer pageSize) {
        Mono<Page<RuleDataVO>> pageMono = ruleService.callApiWithRulePage(HttpMethod.GET, urlService.getRuleUrl(), pageNum, pageSize);
        return pageMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s,"successful"));
            } catch (Exception e) {
                log.error("Failed to call listAllRule Rule API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     *描述当前规则
     */
    @GetMapping("/listOne")
    public Mono<R<DetailRuleVO>> listOneRule(@RequestParam(name = "id") String id) {
        String url = urlService.getRuleUrl() + "/" + id;
        Mono<DetailRuleVO> dynamicEntityMono = ruleService.callApilRule(HttpMethod.GET, url);
        return dynamicEntityMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s,"successful"));
            } catch (Exception e) {
                log.error("Failed to call listOneRule Rule API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     *查看规则状态
     */
    @GetMapping("/status")
    public Mono<R<RuleStatusVO>> lookStatusRule(@RequestParam(name = "id") String id) {
        String url = urlService.getRuleUrl() + "/" + id + "/status";
        Mono<RuleStatusVO> ruleStatusMono = ruleService.callApiRuleStatus(HttpMethod.GET, url);
        return ruleStatusMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s,"successful"));
            } catch (Exception e) {
                log.error("Failed to call lookStatusRule Rule API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     *启动规则
     */
    @PostMapping("/start")
    public Mono<R<String>> startRule(@RequestParam(name = "id") String id) {
        String url = urlService.getRuleUrl() + "/" + id + "/" + "start";
        Mono<String> stringMono = apiService.callApi(HttpMethod.POST, url);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to call start Rule API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     *停止规则
     */
    @PostMapping("/stop")
    public Mono<R<String>> stopRule(@RequestParam(name = "id") String id) {
        String url = urlService.getRuleUrl() + "/" + id + "/" + "stop";
        Mono<String> stringMono = apiService.callApi(HttpMethod.POST, url);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to call stop Rule API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     *重启规则
     */
    @PostMapping("/restart")
    public Mono<R<String>> restartRule(@RequestParam(name = "id") String id) {
        String url = urlService.getRuleUrl() + "/" + id + "/" + "restart";
        Mono<String> stringMono = apiService.callApi(HttpMethod.POST, url);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to restart stop Rule API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     *删除规则
     */
    @DeleteMapping("/delete")
    public Mono<R<String>> deleteRule(@RequestParam(name = "id") String id) {
        String url = urlService.getRuleUrl() + "/" + id;
        Mono<String> stringMono = apiService.callApi(HttpMethod.DELETE, url);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to delete stop Rule API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     *添加actionNodes
     */
    @PostMapping("/createAction")
    public Mono<R<String>> createActions(@RequestBody Object data){
        Mono<String> stringMono = ruleService.addActions(data);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to create Actions API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     *显示actionNodes
     */
    @GetMapping("/listAction")
    public Mono<R<List<Map<String, Map<String, Object>>>>> listActions() {
        Mono<List<Map<String, Map<String, Object>>>> listMono = ruleService.listActions();
        return listMono.flatMap(actions -> {
            try {
                return Mono.just(R.ok(actions));
            } catch (Exception e) {
                log.error("Failed to list Actions API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     *删除actionNodes
     */
    @DeleteMapping("/deleteAction3")
    public Mono<R<String>> deleteActions(@RequestParam(name = "index") Integer  index) {
        Mono<String> stringMono = ruleService.deleteActions(index);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to delete Actions API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     *删除所有actionNodes
     */
    @DeleteMapping("/deleteAllAction")
    public Mono<R<String>> deleteAllActions() {
        Mono<String> stringMono = ruleService.deleteAllActions();
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to delete AllActions API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     *编辑actionNodes
     */
    @PutMapping("/editAction3")
    public Mono<R<String>> editActions(@Valid @RequestBody Object data,@RequestParam(name = "index") Integer index) {
        Mono<String> stringMono = ruleService.editActions(data, index);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to edit Actions API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @DeleteMapping("/deleteAction")
    public Mono<R<String>> deleteActions(@RequestParam(name = "actionType") String  actionType) {
        Mono<String> stringMono = ruleService.deleteActions(actionType);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to delete Actions API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @DeleteMapping("/deleteAction2")
    public Mono<R<String>> deleteActions(@RequestParam(name = "actionType") String  actionType,@RequestParam(name = "index") Integer  index) {
        Mono<String> stringMono = ruleService.deleteActions(actionType,index);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to delete Actions API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @PutMapping("/editAction")
    public Mono<R<String>> editActions(@RequestParam(name = "type") String type,@Valid @RequestBody Object data) {
        Mono<String> stringMono = ruleService.editActions(data, type);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to edit Actions API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }


    @PutMapping("/editAction2")
    public Mono<R<String>> editActions(@RequestParam(name = "type") String type,@Valid @RequestBody Object data,@RequestParam(name = "index") Integer index) {
        Mono<String> stringMono = ruleService.editActions(data, type, index);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to edit Actions API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }


}






