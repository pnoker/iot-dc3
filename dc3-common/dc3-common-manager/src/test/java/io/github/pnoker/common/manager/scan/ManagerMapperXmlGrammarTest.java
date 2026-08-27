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

package io.github.pnoker.common.manager.scan;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Grammar gate for mapper XML: every mapping file on the classpath must parse
 * against the MyBatis mapper DTD. Attributes the bundled DTD does not declare
 * for an element (for example {@code affectData} on {@code insert}) otherwise
 * surface only at application startup, so this gate fails the build instead.
 * No database is involved — parsing alone exercises the DTD validation that
 * {@code sqlSessionFactory} performs.
 */
class ManagerMapperXmlGrammarTest {

    @Test
    void allMappingXmlParsesAgainstTheMybatisDtd() throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapping/*.xml");
        assertThat(resources).isNotEmpty();
        for (Resource resource : resources) {
            Configuration configuration = new Configuration();
            try (InputStream in = resource.getInputStream()) {
                new XMLMapperBuilder(in, configuration, resource.getDescription(), configuration.getSqlFragments()).parse();
            } catch (Exception e) {
                throw new AssertionError("mapper XML rejected by MyBatis: " + resource.getDescription(), e);
            }
        }
    }
}
