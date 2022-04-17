package com.dc3.center.auth.mapper;

import com.dc3.center.auth.bean.Book;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ESBookRepository extends ElasticsearchRepository<Book, String> {
}