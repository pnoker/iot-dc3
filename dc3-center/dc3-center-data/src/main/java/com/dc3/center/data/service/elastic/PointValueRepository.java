package com.dc3.center.data.service.elastic;

import com.dc3.common.bean.point.EsPointValue;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointValueRepository extends ElasticsearchRepository<EsPointValue, String> {
}