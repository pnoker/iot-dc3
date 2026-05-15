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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CmdUtilTest {

    @Test
    void destroyProcessWithCmdWritesCommandBeforeDestroying() {
        Process process = mock(Process.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(process.getOutputStream()).thenReturn(outputStream);

        CmdUtil.destroyProcessWithCmd(process, "quit");

        assertThat(outputStream).hasToString("quit");
        verify(process).destroyForcibly();
    }

    @Test
    void destroyProcessWithCmdAcceptsNullCommand() {
        Process process = mock(Process.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(process.getOutputStream()).thenReturn(outputStream);

        assertThatCode(() -> CmdUtil.destroyProcessWithCmd(process, null)).doesNotThrowAnyException();

        assertThat(outputStream).hasToString("");
        verify(process).destroyForcibly();
    }

}
