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

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-wide guardrails against tests that can pass without verifying production behaviour.
 */
class RepositoryTestQualityGateTest {

    private static final String TEST_JAVA = "/src/test/java/";
    private static final String THIS_FILE = "RepositoryTestQualityGateTest.java";
    private static final List<ForbiddenPattern> FORBIDDEN_PATTERNS = List.of(
            new ForbiddenPattern("disabled test", Pattern.compile("@(Disabled|Ignore)\\b")),
            new ForbiddenPattern("constant boolean assertion", Pattern.compile(
                    "assert(?:True\\(true\\)|False\\(false\\)|That\\((?:true|false)\\)\\.is(?:True|False)\\(\\))")),
            new ForbiddenPattern("no-op schedule test", Pattern.compile(
                    "void\\s+schedule(?:DoesNothing|IsNoOp)\\s*\\(")),
            new ForbiddenPattern("fixture helper self-test", Pattern.compile(
                    "void\\s+(?:driverConfigContains|pointConfigContains)\\w*\\s*\\(")),
            new ForbiddenPattern("comment-only assertion", Pattern.compile(
                    "No exception thrown is the assertion", Pattern.CASE_INSENSITIVE)));
    private static final Pattern ONLINE_TEST_ASSERTS_OFFLINE = Pattern.compile(
            "(?s)void\\s+(?:healthReturnsOnline|healthIsOnline|\\w+OnlineWhen\\w*)\\s*\\([^)]*\\)\\s*\\{"
                    + ".{0,2000}?assertThat\\([^;]*EntityStatusEnum\\.OFFLINE");
    private static final Set<String> REQUIRED_CROSS_CUTTING_COVERAGE = Set.of(
            "dc3-common-api", "dc3-common-facade-grpc", "dc3-common-sql");

    private static void inspect(Path repository, Path path, List<String> violations) {
        try {
            String source = Files.readString(path);
            for (ForbiddenPattern forbidden : FORBIDDEN_PATTERNS) {
                if (forbidden.pattern().matcher(source).find()) {
                    violations.add("%s: %s".formatted(normalized(repository.relativize(path)), forbidden.reason()));
                }
            }
            if (ONLINE_TEST_ASSERTS_OFFLINE.matcher(source).find()) {
                violations.add("%s: online health test asserts OFFLINE"
                        .formatted(normalized(repository.relativize(path))));
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

    private static Set<String> childTexts(Path pom, String containerName, String childName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        Element project = factory.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
        Set<String> values = new HashSet<>();
        NodeList containers = project.getElementsByTagName(containerName);
        for (int containerIndex = 0; containerIndex < containers.getLength(); containerIndex++) {
            Element container = (Element) containers.item(containerIndex);
            NodeList children = container.getElementsByTagName(childName);
            for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                values.add(children.item(childIndex).getTextContent().strip());
            }
        }
        return values;
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }

    @Test
    void javaTestsMustVerifyObservableBehaviour() throws IOException {
        Path repository = findRepositoryRoot();
        List<String> violations = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(repository)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> normalized(path).contains(TEST_JAVA))
                    .filter(path -> path.toString().endsWith("Test.java") || path.toString().endsWith("Tests.java"))
                    .filter(path -> !path.getFileName().toString().equals(THIS_FILE))
                    .forEach(path -> inspect(repository, path, violations));
        }

        assertThat(violations)
                .as("Tests must fail when production behaviour is wrong")
                .isEmpty();
    }

    @Test
    void aggregateCoverageMustIncludeEveryDriverAndCrossCuttingRuntimeModule() throws Exception {
        Path repository = findRepositoryRoot();
        Set<String> driverModules = childTexts(
                repository.resolve("dc3-driver/pom.xml"), "modules", "module");
        Set<String> coveredModules = childTexts(
                repository.resolve("dc3-coverage/pom.xml"), "dependencies", "artifactId");

        assertThat(driverModules).as("Driver reactor must not be empty").isNotEmpty();
        assertThat(coveredModules)
                .as("Aggregate coverage dependencies")
                .containsAll(driverModules)
                .containsAll(REQUIRED_CROSS_CUTTING_COVERAGE);
        assertThat(Files.readString(repository.resolve("dc3-coverage/pom.xml")))
                .as("Generated MapStruct implementations must not dilute coverage")
                .contains("**/entity/builder/*BuilderImpl.class");
    }

    private record ForbiddenPattern(String reason, Pattern pattern) {
    }
}
