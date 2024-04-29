package io.github.ponker.center.ekuiper.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.ponker.center.ekuiper.entity.dto.RecordDto;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

/**
 * @author : Zhen
 * @date : 2024/3/11
 */

public interface ApiService {


    Mono<String> callApi(HttpMethod method, String url);


    Mono<String> callApiWithData(Object data, HttpMethod method, String url);


    Mono<Page<RecordDto>> callApiWithPage(HttpMethod method, String url, Integer current, Integer size);

}
