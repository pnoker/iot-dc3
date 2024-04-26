package io.github.ponker.center.ekuiper.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.ponker.center.ekuiper.entity.vo.DetailRuleVO;
import io.github.ponker.center.ekuiper.entity.vo.RuleDataVO;
import io.github.ponker.center.ekuiper.entity.vo.RuleStatusVO;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @author : Zhen
 * @date : 2024/3/11
 */
public interface RuleService {

    Mono<DetailRuleVO> callApilRule(HttpMethod method, String url);

    Mono<RuleStatusVO> callApiRuleStatus(HttpMethod method, String url);

    Mono<String> callRuleApiWithData(Object data, HttpMethod method, String url) throws Exception;

    Mono<Page<RuleDataVO>> callApiWithRulePage(HttpMethod method, String url, Integer current, Integer size);

    Mono<String> addActions(Object data);

    Mono<List<Map<String, Map<String, Object>>>> listActions() ;

    Mono<String> deleteActions(String actionType);

    Mono<String> deleteActions(String actionType,Integer index);

    Mono<String> deleteActions(Integer index);

    Mono<String> editActions(Object data,String type);

    Mono<String> editActions(Object data, Integer index);

    Mono<String> editActions(Object data,String type, Integer index);


    Mono<String> deleteAllActions();

}
