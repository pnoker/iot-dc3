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

package io.github.pnoker.common.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisPlusConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MybatisPlusConfig.class, TenantLineHandlerImpl.class));

    @Test
    void mybatisPlusInterceptorIsCreatedByDefault() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(MybatisPlusInterceptor.class));
    }

    @Test
    void mybatisPlusInterceptorBacksOffWhenUserBeanExists() {
        MybatisPlusInterceptor customInterceptor = new MybatisPlusInterceptor();

        contextRunner.withBean(MybatisPlusInterceptor.class, () -> customInterceptor)
                .run(context -> assertThat(context.getBean(MybatisPlusInterceptor.class)).isSameAs(customInterceptor));
    }

}
