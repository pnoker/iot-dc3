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

package io.github.pnoker.common.agentic.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticPropertiesTest {

    @Test
    void defaultsMatchProductionExpectations() {
        AgenticProperties properties = new AgenticProperties();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getMemoryMaxMessages()).isEqualTo(50);
        assertThat(properties.isMemoryEnabled()).isFalse();
        assertThat(properties.isToolCallingEnabled()).isFalse();
        assertThat(properties.getAttachmentStoragePath()).isEqualTo("dc3/data/upload/agentic/attachment");
    }

    @Test
    void settersFlowToGetters() {
        AgenticProperties properties = new AgenticProperties();
        properties.setEnabled(false);
        properties.setMemoryMaxMessages(100);
        properties.setMemoryEnabled(true);
        properties.setToolCallingEnabled(true);
        properties.setAttachmentStoragePath("/tmp/agentic");

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getMemoryMaxMessages()).isEqualTo(100);
        assertThat(properties.isMemoryEnabled()).isTrue();
        assertThat(properties.isToolCallingEnabled()).isTrue();
        assertThat(properties.getAttachmentStoragePath()).isEqualTo("/tmp/agentic");
    }
}
