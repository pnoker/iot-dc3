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

package io.github.pnoker.common.thread.entity.property;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadPropertiesTest {

    @Test
    void defaultsApplyWhenNoOverridesProvided() {
        ThreadProperties props = new ThreadProperties();
        assertThat(props.getPrefix()).isEqualTo("dc3-thread-");
        assertThat(props.getCorePoolSize()).isEqualTo(4);
        assertThat(props.getMaximumPoolSize()).isEqualTo(32);
        assertThat(props.getKeepAliveTime()).isEqualTo(15);
        assertThat(props.isPoolSizeValid()).isTrue();
    }

    @Test
    void poolSizeValidationRequiresMaxToBeAtLeastCore() {
        ThreadProperties props = new ThreadProperties();
        props.setCorePoolSize(8);
        props.setMaximumPoolSize(4);
        assertThat(props.isPoolSizeValid()).isFalse();
        props.setMaximumPoolSize(8);
        assertThat(props.isPoolSizeValid()).isTrue();
        props.setMaximumPoolSize(16);
        assertThat(props.isPoolSizeValid()).isTrue();
    }
}
