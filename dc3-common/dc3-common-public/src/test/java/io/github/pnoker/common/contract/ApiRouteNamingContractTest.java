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

package io.github.pnoker.common.contract;

import io.github.pnoker.common.constant.common.SymbolConstant;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the public HTTP contract:
 * paths use lower snake_case and dynamic values are passed as query params.
 */
class ApiRouteNamingContractTest {

    private static final Pattern MAPPING = Pattern.compile(
            "@(?:Get|Post|Put|Delete|Patch|Request)Mapping\\s*\\((.*?)\\)",
            Pattern.DOTALL);
    private static final Pattern METHOD_MAPPING = Pattern.compile(
            "@(Get|Post|Put|Delete|Patch|Request)Mapping(?:\\s*\\((.*?)\\))?\\s*(?:(?:\\r?\\n)\\s*@[^\r\n]+)*\\s*public\\s+[^=;{]+?\\s+([A-Za-z0-9_]+)\\s*\\(",
            Pattern.DOTALL);
    private static final Pattern STRING_LITERAL = Pattern.compile("\"([^\"]*)\"");
    private static final Pattern REQUEST_PARAM = Pattern.compile("@RequestParam\\s*\\((.*?)\\)", Pattern.DOTALL);
    private static final Pattern NAMED_REQUEST_PARAM = Pattern.compile("(?:value|name)\\s*=\\s*\"([^\"]+)\"");

    private static List<Path> javaSources() throws IOException {
        Path commonRoot = Path.of("..").toAbsolutePath().normalize();
        try (var stream = Files.walk(commonRoot)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("/src/main/java/"))
                    .filter(path -> path.toString().contains("/controller/"))
                    .toList();
        }
    }

    private static void assertRoute(Path source, String route, List<String> violations) {
        if (!route.contains(SymbolConstant.SLASH)) {
            return;
        }
        if (route.contains("/{")) {
            violations.add("%s uses path variable route %s".formatted(source, route));
        }
        for (String segment : route.split(SymbolConstant.SLASH)) {
            if (segment.isBlank() || segment.startsWith("{")) {
                continue;
            }
            if (segment.matches(".*[A-Z-].*")) {
                violations.add("%s uses non-snake-case route segment %s in %s".formatted(source, segment, route));
            }
            if (segment.matches("id|ids|name|code|service|[a-z0-9]+_id")) {
                violations.add("%s uses non-semantic route segment %s in %s".formatted(source, segment, route));
            }
        }
    }

    private static void assertMethodRoute(Path source, String annotation, String annotationArgs, String method,
                                          List<String> violations) {
        if ("Request".equals(annotation)) {
            return;
        }

        String route = "";
        if (annotationArgs != null) {
            Matcher literal = STRING_LITERAL.matcher(annotationArgs);
            if (literal.find()) {
                route = literal.group(1);
            }
        }
        if (route.isBlank()) {
            violations.add("%s maps method %s with an empty route".formatted(source, method));
        }
    }

    private static void assertRequestParam(Path source, String annotationArgs, List<String> violations) {
        Matcher named = NAMED_REQUEST_PARAM.matcher(annotationArgs);
        if (named.find()) {
            assertParamName(source, named.group(1), violations);
            return;
        }

        Matcher positional = STRING_LITERAL.matcher(annotationArgs);
        if (positional.find()) {
            assertParamName(source, positional.group(1), violations);
        }
    }

    private static void assertParamName(Path source, String name, List<String> violations) {
        if (name.matches(".*[A-Z-].*")) {
            violations.add("%s uses non-snake-case request param %s".formatted(source, name));
        }
    }

    @Test
    void controllerRoutesUseLowerSnakeCaseAndQueryParams() throws IOException {
        List<String> violations = new ArrayList<>();

        for (Path source : javaSources()) {
            String text = Files.readString(source);
            if (text.contains("PathVariable")) {
                violations.add("%s imports or uses PathVariable".formatted(source));
            }
            if (text.contains("AttachmentUploadRequest")) {
                violations.add("%s uses JSON/Base64 attachment upload request instead of multipart file upload".formatted(source));
            }
            if (text.contains("MultipartFile")) {
                violations.add("%s uses MultipartFile; reactive controllers should use FilePart".formatted(source));
            }
            if (text.contains("FileUtil.getTempPath() +")) {
                violations.add("%s concatenates upload temp paths instead of using module-scoped FileUtil helpers".formatted(source));
            }

            Matcher mappings = MAPPING.matcher(text);
            while (mappings.find()) {
                Matcher literals = STRING_LITERAL.matcher(mappings.group(1));
                while (literals.find()) {
                    assertRoute(source, literals.group(1), violations);
                }
            }

            Matcher requestParams = REQUEST_PARAM.matcher(text);
            while (requestParams.find()) {
                assertRequestParam(source, requestParams.group(1), violations);
            }

            Matcher methodMappings = METHOD_MAPPING.matcher(text);
            while (methodMappings.find()) {
                assertMethodRoute(source, methodMappings.group(1), methodMappings.group(2), methodMappings.group(3),
                        violations);
            }
        }

        assertThat(violations).isEmpty();
    }

}
