package com.pnoker.center.zipkin.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName: TagsProvideBean
 * @Description: TODO
 * @author: Created by xxx <a href="xxx@163.com">Contact author</a>
 * @date: 2019/2/27 13:54
 * @Version: V1.0
 */
@Configuration
public class TagsProvideBean {

    /**
     * 将MyTagsProvider注入
     *
     * @return
     */
    @Bean
    public MyTagsProvider myTagsProvider() {
        return new MyTagsProvider();
    }

}