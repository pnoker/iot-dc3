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
package io.github.pnoker.db.agentic;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Layering guard: the database adapters must stay pure SQL ports. They implement the
 * {@code Reactive*Store} contracts of {@code dc3-common-agentic} and must never import or
 * orchestrate service-layer implementation classes (business rules, filesystem cleanup), so the
 * dependency direction never reverses.
 */
class AgenticStoreLayeringTest {

    @Test
    void storeAdaptersNeverImportServiceImplementations() throws IOException {
        List<Path> offenders;
        try (Stream<Path> sources = Files.walk(Path.of("src", "main", "java"))) {
            offenders = sources.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsServiceImport(path))
                    .toList();
        }
        assertThat(offenders)
                .as("db adapters must not reference io.github.pnoker.common.agentic.service.impl")
                .isEmpty();
    }

    private boolean containsServiceImport(Path path) {
        try {
            return Files.readString(path).contains("io.github.pnoker.common.agentic.service.impl");
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read " + path, exception);
        }
    }
}
