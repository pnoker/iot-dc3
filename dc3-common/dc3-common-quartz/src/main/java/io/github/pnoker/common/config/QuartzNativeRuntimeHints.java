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

import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleInstanceIdGenerator;
import org.quartz.simpl.SimpleJobFactory;
import org.quartz.simpl.SimpleThreadPool;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM native-image hints for Quartz. StdSchedulerFactory instantiates its
 * pluggable pieces from class names carried in quartz properties —
 * application-quartz.yml sets {@code instanceId: AUTO} (resolves to
 * SimpleInstanceIdGenerator) and {@code threadPool.class=SimpleThreadPool},
 * and the default job store and job factory are string-resolved the same
 * way. None of these reflective constructions is covered by Spring AOT or
 * the reachability metadata repository, so a native image without these
 * registrations dies during scheduler boot with ClassNotFoundException.
 *
 * @author pnoker
 */
public class QuartzNativeRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection().registerType(SimpleInstanceIdGenerator.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
        hints.reflection().registerType(SimpleThreadPool.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
        hints.reflection().registerType(RAMJobStore.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
        hints.reflection().registerType(SimpleJobFactory.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
        hints.reflection().registerType(StdSchedulerFactory.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
    }
}
