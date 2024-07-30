package io.github.ponker.center.ekuiper.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.ponker.center.ekuiper.entity.vo.PortableDataVO;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

public interface PortableService {

    Mono<Page<PortableDataVO>> callApiWithPortableData(HttpMethod get, String portableUrl, Integer pageNum, Integer pageSize);

    Mono<PortableDataVO> callApiWithPortable(HttpMethod get, String url);
}
