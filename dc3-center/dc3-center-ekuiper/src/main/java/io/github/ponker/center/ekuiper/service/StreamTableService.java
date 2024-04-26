package io.github.ponker.center.ekuiper.service;

import io.github.ponker.center.ekuiper.entity.vo.DetailStreamVO;
import io.github.ponker.center.ekuiper.entity.vo.DetailTableVO;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

/**
 * @author : Zhen
 * @date : 2024/3/11
 */
public interface StreamTableService {

    Mono<DetailStreamVO> callApiStream(HttpMethod method, String url);

    Mono<DetailTableVO> callApiTable(HttpMethod method, String url);

}
