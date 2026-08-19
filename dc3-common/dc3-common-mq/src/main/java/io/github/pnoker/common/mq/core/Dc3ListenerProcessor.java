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

package io.github.pnoker.common.mq.core;

import io.github.pnoker.common.constant.common.RequestIdConstant;
import io.github.pnoker.common.mq.MqHeaders;
import io.github.pnoker.common.mq.adapter.BrokerAdapter;
import io.github.pnoker.common.mq.adapter.RawBatchListener;
import io.github.pnoker.common.mq.adapter.RawDeliveryListener;
import io.github.pnoker.common.mq.adapter.WireMqDelivery;
import io.github.pnoker.common.mq.annotation.Dc3Listener;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import io.github.pnoker.common.constant.mq.DeliveryMode;
import io.github.pnoker.common.mq.subscription.SubscriptionSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processes {@link Dc3Listener @Dc3Listener} methods after all singletons exist and
 * registers each one with the active {@link BrokerAdapter}. Mirrors how
 * {@code @RabbitListener} is processed today, minus the broker API: payload types are
 * resolved from the method signature, {@code ${...}} placeholders in keyPattern/group
 * resolve against the environment (drivers name their per-instance queues this way),
 * and the request id header is restored into the MDC around every invocation.
 *
 * @author pnoker
 * @since 2026.8.19
 */
@Slf4j
@RequiredArgsConstructor
public class Dc3ListenerProcessor implements SmartInitializingSingleton, ApplicationContextAware {

    private final BrokerAdapter adapter;

    private ApplicationContext applicationContext;

    /**
     * Cache of classes already scanned for listener methods.
     */
    private final Map<Class<?>, List<Method>> listenerMethodCache = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        int registered = 0;
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (Method method : listenerMethods(targetClass)) {
                Dc3Listener annotation = method.getAnnotation(Dc3Listener.class);
                if (Objects.isNull(annotation)) {
                    continue;
                }
                register(bean, method, annotation);
                registered++;
            }
        }
        log.info("Dc3Listener subscriptions registered, count={}, broker={}", registered, adapter.type());
    }

    private List<Method> listenerMethods(Class<?> targetClass) {
        return listenerMethodCache.computeIfAbsent(targetClass, clazz -> {
            List<Method> methods = new ArrayList<>();
            for (Class<?> current = clazz; Objects.nonNull(current) && current != Object.class;
                    current = current.getSuperclass()) {
                for (Method method : current.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Dc3Listener.class) && !Modifier.isStatic(method.getModifiers())) {
                        method.setAccessible(true);
                        methods.add(method);
                    }
                }
            }
            return methods;
        });
    }

    private void register(Object bean, Method method, Dc3Listener annotation) {
        Environment environment = applicationContext.getEnvironment();
        String keyPattern = environment.resolvePlaceholders(annotation.keyPattern());
        String group = environment.resolvePlaceholders(annotation.group());

        ResolvableType parameter = ResolvableType.forMethodParameter(method, 0);
        Class<?> acknowledgmentType = ResolvableType.forMethodParameter(method, 1).toClass();
        if (acknowledgmentType != Acknowledgment.class) {
            throw new IllegalStateException("Second parameter of @Dc3Listener method must be Acknowledgment: "
                    + method);
        }

        boolean batch = parameter.toClass() == List.class;
        Class<?> payloadType;
        if (batch) {
            ResolvableType received = parameter.getGeneric(0);
            if (received.toClass() != MqReceived.class) {
                throw new IllegalStateException(
                        "Batch @Dc3Listener parameter must be List<MqReceived<T>>: " + method);
            }
            payloadType = received.getGeneric(0).resolve(Object.class);
        } else if (parameter.toClass() == MqReceived.class) {
            payloadType = parameter.getGeneric(0).resolve(Object.class);
        } else {
            throw new IllegalStateException(
                    "@Dc3Listener parameter must be MqReceived<T> or List<MqReceived<T>>: " + method);
        }

        SubscriptionSpec spec = new SubscriptionSpec(annotation.topic(), annotation.mode(), annotation.profile(),
                batch ? DeliveryMode.BATCH : annotation.delivery(), keyPattern, group, null, payloadType,
                annotation.deadLetter());

        if (batch) {
            adapter.subscribeBatch(spec, batchBridge(bean, method, payloadType));
        } else {
            adapter.subscribe(spec, deliveryBridge(bean, method, payloadType));
        }
        log.info("Dc3Listener registered, bean={}, method={}, topic={}, mode={}, delivery={}, keyPattern={}",
                method.getDeclaringClass().getSimpleName(), method.getName(), annotation.topic(),
                annotation.mode(), spec.delivery(), keyPattern);
    }

    @SuppressWarnings("unchecked")
    private RawDeliveryListener deliveryBridge(Object bean, Method method, Class<?> payloadType) {
        return delivery -> {
            Object payload = EnvelopeCodec.deserialize(delivery, payloadType);
            MqReceived<Object> received = new MqReceived<>(payload, delivery.headers(), delivery.redelivered());
            invokeWithMdc(bean, method, received, delivery.acknowledgment(), firstRequestId(delivery));
        };
    }

    @SuppressWarnings("unchecked")
    private RawBatchListener batchBridge(Object bean, Method method, Class<?> payloadType) {
        return batch -> {
            if (batch.isEmpty()) {
                return;
            }
            Acknowledgment acknowledgment = batch.get(0).acknowledgment();
            List<MqReceived<Object>> received = new ArrayList<>(batch.size());
            for (WireMqDelivery delivery : batch) {
                Object payload = EnvelopeCodec.deserialize(delivery, payloadType);
                received.add(new MqReceived<>(payload, delivery.headers(), delivery.redelivered()));
            }
            invokeWithMdc(bean, method, received, acknowledgment, firstRequestId(batch.get(0)));
        };
    }

    private void invokeWithMdc(Object bean, Method method, Object argument, Acknowledgment acknowledgment,
                               String requestId) {
        if (Objects.nonNull(requestId)) {
            MDC.put(RequestIdConstant.MDC_KEY, requestId);
        }
        try {
            method.invoke(bean, argument, acknowledgment);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Dc3Listener invocation failed: " + method, cause);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Dc3Listener method not accessible: " + method, e);
        } finally {
            if (Objects.nonNull(requestId)) {
                MDC.remove(RequestIdConstant.MDC_KEY);
            }
        }
    }

    private static String firstRequestId(WireMqDelivery delivery) {
        return Objects.nonNull(delivery.headers()) ? delivery.headers().get(MqHeaders.REQUEST_ID) : null;
    }
}
