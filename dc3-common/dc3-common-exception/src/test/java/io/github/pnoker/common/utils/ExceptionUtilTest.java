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

import io.github.pnoker.common.constant.common.ExceptionConstant;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExceptionUtilTest {

    @Test
    void getNotAvailableServiceMessageReturnsExistingMessageUnchanged() {
        assertThat(ExceptionUtil.getNotAvailableServiceMessage("manager", "explicit"))
                .isEqualTo("explicit");
    }

    @Test
    void getNotAvailableServiceMessageBuildsFallbackForBlankMessage() {
        String message = ExceptionUtil.getNotAvailableServiceMessage("manager", "");
        assertThat(message).contains(ExceptionConstant.NO_AVAILABLE_SERVER).contains("manager");
    }

    @Test
    void getNotAvailableServiceMessageBuildsFallbackForNullMessage() {
        String message = ExceptionUtil.getNotAvailableServiceMessage("manager", null);
        assertThat(message).contains(ExceptionConstant.NO_AVAILABLE_SERVER).contains("manager");
    }

    @Test
    void utilityClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<ExceptionUtil> constructor = ExceptionUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
