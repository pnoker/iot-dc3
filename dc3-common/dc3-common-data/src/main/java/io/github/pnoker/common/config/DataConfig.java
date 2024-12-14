package io.github.pnoker.common.config;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ComponentScan(basePackages = {
        "io.github.pnoker.common.data.*"
})
@MapperScan(basePackages = {
        "io.github.pnoker.common.data.mapper"
})
public class DataConfig {
}
