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

package io.github.pnoker.common.log;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-wide source gate for stable, safe and consistently English application logs.
 */
class LoggingPolicyTest {

    private static final Pattern FORBIDDEN_PRINT = Pattern.compile(
            "System\\.(?:out|err)\\.(?:print|println|printf)\\s*\\(|\\.printStackTrace\\s*\\(");
    private static final Pattern LOG_CALL = Pattern.compile(
            "\\b(?:log|logger|LOGGER)\\.(?:trace|debug|info|warn|error)\\s*\\(");
    private static final Pattern STRING_LITERAL = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"", Pattern.DOTALL);
    private static final Pattern HAN_CHARACTER = Pattern.compile("\\p{IsHan}");
    private static final Pattern RAW_SENSITIVE_FIELD = Pattern.compile(
            "(?i)\\b(?:payload|body|response|headers|principalHeader)\\s*=\\s*\\{\\}");

    @Test
    void productionJavaSourcesFollowLoggingPolicy() throws IOException {
        Path repository = findRepositoryRoot();
        List<String> violations = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(repository)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().replace('\\', '/').contains("/src/main/java/"))
                    .forEach(path -> inspect(repository, path, violations));
        }

        assertThat(violations)
                .as("Logging policy violations")
                .isEmpty();
    }

    private static void inspect(Path repository, Path path, List<String> violations) {
        try {
            String source = Files.readString(path);
            Matcher printMatcher = FORBIDDEN_PRINT.matcher(source);
            while (printMatcher.find()) {
                violations.add(location(repository, path, source, printMatcher.start())
                        + " use SLF4J instead of console printing");
            }

            Matcher callMatcher = LOG_CALL.matcher(source);
            while (callMatcher.find()) {
                int end = findInvocationEnd(source, callMatcher.end());
                if (end < 0) {
                    violations.add(location(repository, path, source, callMatcher.start())
                            + " logging invocation cannot be parsed");
                    continue;
                }
                String invocation = source.substring(callMatcher.end(), end);
                String firstArgument = firstArgument(invocation).strip();
                String where = location(repository, path, source, callMatcher.start());

                if (!firstArgument.startsWith("\"")) {
                    violations.add(where + " log message must be a stable string literal");
                } else if (containsOperatorOutsideString(firstArgument, '+')) {
                    violations.add(where + " use SLF4J placeholders instead of message concatenation");
                }

                Matcher literalMatcher = STRING_LITERAL.matcher(invocation);
                while (literalMatcher.find()) {
                    String literal = literalMatcher.group();
                    if (HAN_CHARACTER.matcher(literal).find()) {
                        violations.add(where + " log message must be English");
                    }
                    if (RAW_SENSITIVE_FIELD.matcher(literal).find()) {
                        violations.add(where + " log metadata instead of raw payloads or security-sensitive fields");
                    }
                }
                if (invocation.contains("JsonUtil.toJsonString(")) {
                    violations.add(where + " do not serialize complete domain objects into logs");
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to inspect " + path, e);
        }
    }

    private static Path findRepositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null && !Files.exists(current.resolve(".git"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("Cannot locate the repository root from " + System.getProperty("user.dir"));
        }
        return current;
    }

    private static String firstArgument(String invocation) {
        boolean string = false;
        boolean character = false;
        boolean escaped = false;
        int depth = 0;
        for (int i = 0; i < invocation.length(); i++) {
            char current = invocation.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if ((string || character) && current == '\\') {
                escaped = true;
                continue;
            }
            if (!character && current == '"') {
                string = !string;
                continue;
            }
            if (!string && current == '\'') {
                character = !character;
                continue;
            }
            if (string || character) {
                continue;
            }
            if (current == '(' || current == '[' || current == '{') {
                depth++;
            } else if (current == ')' || current == ']' || current == '}') {
                depth--;
            } else if (current == ',' && depth == 0) {
                return invocation.substring(0, i);
            }
        }
        return invocation;
    }

    private static boolean containsOperatorOutsideString(String value, char operator) {
        return STRING_LITERAL.matcher(value).replaceAll("").indexOf(operator) >= 0;
    }

    private static int findInvocationEnd(String source, int start) {
        boolean string = false;
        boolean character = false;
        boolean escaped = false;
        int depth = 1;
        for (int i = start; i < source.length(); i++) {
            char current = source.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if ((string || character) && current == '\\') {
                escaped = true;
                continue;
            }
            if (!character && current == '"') {
                string = !string;
                continue;
            }
            if (!string && current == '\'') {
                character = !character;
                continue;
            }
            if (string || character) {
                continue;
            }
            if (current == '(') {
                depth++;
            } else if (current == ')' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static String location(Path repository, Path path, String source, int offset) {
        long line = source.substring(0, offset).lines().count() + 1;
        return repository.relativize(path).toString().replace('\\', '/') + ':' + line;
    }
}
