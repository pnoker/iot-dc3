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

package io.github.pnoker.common.resource.registrar.scan;

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Build-time gate: classpath-scans {@code @RestController} classes under a base package and runs
 * {@link ApiAnnotationValidator} on every handler method. Returns the aggregated defect list so a
 * per-service test can assert it is empty (the SP2 ratchet allowlist). No Spring context, no running
 * service — pure reflection over compiled classes.
 */
public class ControllerAnnotationGate {

    @SuppressWarnings("unchecked")
    private static final Class<? extends Annotation>[] MAPPING_ANNOTATIONS = new Class[]{
            RequestMapping.class, GetMapping.class, PostMapping.class,
            PutMapping.class, DeleteMapping.class, PatchMapping.class};

    private final ApiAnnotationValidator validator = new ApiAnnotationValidator();

    private static boolean hasMapping(Method method) {
        for (Class<? extends Annotation> annotation : MAPPING_ANNOTATIONS) {
            if (method.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Scan a base package for {@code @RestController} classes and validate each handler
     * method's API annotations, aggregating all defects.
     *
     * @param basePackage the package to scan
     * @return the aggregated list of defects
     */
    public List<String> validatePackage(String basePackage) {
        List<String> defects = new ArrayList<>();
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        scanner.findCandidateComponents(basePackage).forEach(bean -> {
            Class<?> controller;
            try {
                controller = Class.forName(bean.getBeanClassName());
            } catch (ClassNotFoundException ignored) {
                defects.add(bean.getBeanClassName() + ": cannot load controller class");
                return;
            }
            for (Method method : controller.getDeclaredMethods()) {
                if (!hasMapping(method)) {
                    continue;
                }
                String label = controller.getSimpleName() + "#" + method.getName();
                defects.addAll(validator.validate(label, method));
            }
        });
        return defects;
    }
}
