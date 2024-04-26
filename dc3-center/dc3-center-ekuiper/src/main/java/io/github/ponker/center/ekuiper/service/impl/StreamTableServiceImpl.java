package io.github.ponker.center.ekuiper.service.impl;

import io.github.ponker.center.ekuiper.entity.vo.DetailStreamVO;
import io.github.ponker.center.ekuiper.entity.vo.DetailTableVO;
import io.github.ponker.center.ekuiper.service.StreamTableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author : Zhen
 * @date : 2024/3/11
 */
@Slf4j
@Service
public class StreamTableServiceImpl implements StreamTableService {

    @Autowired
    private WebClient webClient;

    @Override
    public Mono<DetailStreamVO> callApiStream(HttpMethod method, String url) {
        return webClient
                .method(method)
                .uri(url)
                .retrieve()
                .bodyToMono(DetailStreamVO.class);
    }

    @Override
    public Mono<DetailTableVO> callApiTable(HttpMethod method, String url) {
        return webClient
                .method(method)
                .uri(url)
                .retrieve()
                .bodyToMono(DetailTableVO.class);
    }
}
