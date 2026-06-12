/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.auth.generator;

import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.google.common.collect.ImmutableMap;
import io.github.pnoker.common.utils.MybatisUtil;

/**
 *
 * <p>
 * :
 * <p>
 * MyBatis code generator for the auth module.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public class MybatisGenerator {

    public static void main(String[] args) {
        generator();
    }

    public static void generator() {
        String path = System.getProperty("user.dir") + "/dc3-common/dc3-common-auth/src/main";
        MybatisUtil.defaultGenerator("dc3-postgres", "35432", "dc3", "dc3_auth", "dc3", "dc3dc3dc3")
                .globalConfig(builder -> MybatisUtil.defaultGlobalConfig(builder, path))
                .dataSourceConfig(MybatisUtil::defaultDataSourceConfig)
                .packageConfig(builder -> builder.parent("io.github.pnoker.common.auth")
                        .entity("entity.model")
                        .service("dal")
                        .serviceImpl("dal.impl")
                        .mapper("mapper")
                        .pathInfo(ImmutableMap.of(OutputFile.service, path + "/java/io/github/pnoker/common/auth/dal",
                                OutputFile.serviceImpl, path + "/java/io/github/pnoker/common/auth/dal/impl", OutputFile.xml,
                                path + "/resources/mapping")))
                .templateEngine(new FreemarkerTemplateEngine())
                .strategyConfig(MybatisUtil::defaultStrategyConfig)
                .strategyConfig(builder -> builder.addInclude("dc3_api", "dc3_menu", "dc3_resource", "dc3_role",
                        "dc3_role_resource_bind", "dc3_role_principal_bind", "dc3_driver_token", "dc3_tenant",
                        "dc3_principal", "dc3_user", "dc3_local_credential", "dc3_tenant_membership",
                        "dc3_service_account", "dc3_identity_provider", "dc3_external_identity"))
                .execute();
    }

}
