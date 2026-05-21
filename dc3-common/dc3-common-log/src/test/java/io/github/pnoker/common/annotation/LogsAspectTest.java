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

package io.github.pnoker.common.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogsAspectTest {

    private final LogsAspect logsAspect = new LogsAspect();

    private static ProceedingJoinPoint joinPoint(String methodName) {
        Signature signature = mock(Signature.class);
        when(signature.getDeclaringType()).thenReturn(AnnotatedOperations.class);
        when(signature.getName()).thenReturn(methodName);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        return joinPoint;
    }

    private static java.lang.reflect.Method annotatedMethod(String name) throws NoSuchMethodException {
        return AnnotatedOperations.class.getDeclaredMethod(name);
    }

    @Test
    void doAroundReturnsProceedResult() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint("create");
        Logs logs = annotatedMethod("defaultOperation").getAnnotation(Logs.class);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = logsAspect.doAround(joinPoint, logs);

        assertThat(result).isEqualTo("ok");
        verify(joinPoint).proceed();
    }

    @Test
    void doAroundRethrowsProceedFailure() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint("delete");
        Logs logs = annotatedMethod("warningOperation").getAnnotation(Logs.class);
        IllegalStateException failure = new NoStackTraceException("boom");
        when(joinPoint.proceed()).thenThrow(failure);

        assertThatThrownBy(() -> logsAspect.doAround(joinPoint, logs))
                .isSameAs(failure);
        verify(joinPoint).proceed();
    }

    @Test
    void logsAnnotationKeepsRuntimeMethodContract() throws NoSuchMethodException {
        Logs logs = annotatedMethod("defaultOperation").getAnnotation(Logs.class);

        assertThat(logs.value()).isEqualTo("sync-resource");
        assertThat(logs.type()).isEqualTo(LogsType.INFO);
        assertThat(logs.tag()).isEmpty();
        assertThat(logs.save()).isFalse();
        assertThat(Logs.class.getAnnotation(Retention.class).value())
                .isEqualTo(java.lang.annotation.RetentionPolicy.RUNTIME);
        assertThat(Logs.class.getAnnotation(Target.class).value()).containsExactly(ElementType.METHOD);
    }

    @Test
    void logsAnnotationAllowsExplicitMetadata() throws NoSuchMethodException {
        Logs logs = annotatedMethod("warningOperation").getAnnotation(Logs.class);

        assertThat(logs.value()).isEqualTo("warn-resource");
        assertThat(logs.type()).isEqualTo(LogsType.WARN);
        assertThat(logs.tag()).isEqualTo("resource");
        assertThat(logs.save()).isTrue();
        assertThat(LogsType.values())
                .containsExactly(LogsType.INFO, LogsType.WARN, LogsType.DEBUG, LogsType.ERROR);
    }

    private static final class AnnotatedOperations {

        @Logs("sync-resource")
        void defaultOperation() {
        }

        @Logs(value = "warn-resource", type = LogsType.WARN, tag = "resource", save = true)
        void warningOperation() {
        }

    }

    private static final class NoStackTraceException extends IllegalStateException {

        private NoStackTraceException(String message) {
            super(message);
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }

    }

}
