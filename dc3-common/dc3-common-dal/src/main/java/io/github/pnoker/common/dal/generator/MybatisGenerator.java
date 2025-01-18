/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.dal.generator;

import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.google.common.collect.ImmutableMap;
import io.github.pnoker.common.utils.MybatisUtil;

/**
 * 自动代码生成工具
 * <p>
 * 注意:
 * <p>
 * 当前配置仅用于 dc3-common-dal 服务模块, 如果需要用于其他模块请重新配置 path 参数。
 *
 * @author pnoker
 * @version 2024.3.10
 * @since 2022.1.0
 */
public class MybatisGenerator {
    public static void main(String[] args) {
        generator();
    }

    public static void generator() {
        String path = System.getProperty("user.dir") + "/dc3-common/dc3-common-dal/src/main";
        MybatisUtil.defaultGenerator("localhost", "35432", "dc3", "dc3_auth", "dc3", "dc3dc3dc3")
                .globalConfig(builder -> MybatisUtil.defaultGlobalConfig(builder, path))
                .dataSourceConfig(MybatisUtil::defaultDataSourceConfig)
                .packageConfig(builder -> builder
                        .parent("io.github.pnoker.common.dal")
                        .entity("entity.model")
                        .service("dal")
                        .serviceImpl("dal.impl")
                        .mapper("mapper")
                        .pathInfo(ImmutableMap.of(
                                OutputFile.service, path + "/java/io/github/pnoker/common/dal/dal",
                                OutputFile.serviceImpl, path + "/java/io/github/pnoker/common/dal/dal/impl",
                                OutputFile.xml, path + "/resources/mapping"))
                )
                .templateEngine(new FreemarkerTemplateEngine())
                .strategyConfig(MybatisUtil::defaultStrategyConfig)
                .strategyConfig(builder -> builder
                        .addInclude(
                                "dc3_group",
                                "dc3_group_bind",
                                "dc3_label",
                                "dc3_label_bind"
                        )
                ).execute();
    }
}