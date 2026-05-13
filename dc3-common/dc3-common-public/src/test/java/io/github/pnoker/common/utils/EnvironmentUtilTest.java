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

package io.github.pnoker.common.utils;

import io.github.pnoker.common.constant.common.EnvironmentConstant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvironmentUtilTest {

    @Test
    void getNodeIdReturnsUuidString() {
        String nodeId = EnvironmentUtil.getNodeId();
        assertThat(UUID.fromString(nodeId).toString()).isEqualTo(nodeId);
    }

    @Test
    void getNodeIdIsUniquePerCall() {
        assertThat(EnvironmentUtil.getNodeId()).isNotEqualTo(EnvironmentUtil.getNodeId());
    }

    @Test
    void isDevReturnsTrueOnlyForDevEnv() {
        assertThat(EnvironmentUtil.isDev(EnvironmentConstant.ENV_DEV)).isTrue();
        assertThat(EnvironmentUtil.isDev("prod")).isFalse();
        assertThat(EnvironmentUtil.isDev(null)).isFalse();
    }

    @Test
    void getTagInDevEnvironmentBuildsLowerCasedPrefix() {
        String tag = EnvironmentUtil.getTag(EnvironmentConstant.ENV_DEV, "GroupA");
        assertThat(tag).isEqualTo(EnvironmentConstant.ENV_DEV.toLowerCase() + "_groupa.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"prod", "test", "stage"})
    void getTagOutsideDevReturnsEmpty(String env) {
        assertThat(EnvironmentUtil.getTag(env, "GroupA")).isEmpty();
    }

    @Test
    void utilityClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<EnvironmentUtil> constructor = EnvironmentUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
