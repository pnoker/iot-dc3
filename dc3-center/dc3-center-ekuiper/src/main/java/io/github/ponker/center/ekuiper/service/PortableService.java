package io.github.ponker.center.ekuiper.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import io.github.ponker.center.ekuiper.entity.po.Portable;
import io.github.ponker.center.ekuiper.entity.vo.PortableDataVO;
import io.github.ponker.center.ekuiper.mapper.PortableMapper;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

public interface PortableService extends IService<Portable> {

    Mono<Page<PortableDataVO>> callApiWithPortableData(HttpMethod get, String portableUrl, Integer pageNum, Integer pageSize);

    Mono<PortableDataVO> callApiWithPortable(HttpMethod get, String url);

    Mono<String> callApiWithPortableCreate(Object form, HttpMethod post, String portableUrl);

    void deletePortable(String name);
}
