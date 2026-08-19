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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-wide guardrails for the low-level common module boundaries.
 */
class CommonModuleBoundaryTest {

    private static final String MAIN_JAVA = "/src/main/java/";
    private static final String COMMON_CONSTANT_SOURCE =
            "dc3-common/dc3-common-constant/src/main/java/";
    private static final Pattern DC3_PACKAGE = Pattern.compile(
            "(?m)^package\\s+io\\.github\\.pnoker(?:\\.|;)");
    private static final Pattern TOP_LEVEL_SHARED_TYPE = Pattern.compile(
            "(?m)^public\\s+(?:enum\\s+[A-Za-z_$][A-Za-z0-9_$]*"
                    + "|(?:final\\s+)?class\\s+[A-Za-z_$][A-Za-z0-9_$]*Constant)\\b");

    @Test
    void sharedTopLevelEnumsAndConstantClassesStayInCommonConstant() throws IOException {
        Path repository = findRepositoryRoot();
        List<String> violations = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(repository)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> normalized(path).contains(MAIN_JAVA))
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> inspectSharedTypePlacement(repository, path, violations));
        }

        assertThat(violations)
                .as("Shared type placement violations")
                .isEmpty();
    }

    @Test
    void foundationModulesKeepTheirDependencyFloor() throws Exception {
        Path common = findRepositoryRoot().resolve("dc3-common");

        assertInternalDependencies(
                common.resolve("dc3-common-constant/pom.xml"),
                Set.of());
        assertInternalDependencies(
                common.resolve("dc3-common-public/pom.xml"),
                Set.of("dc3-common-constant", "dc3-common-exception"));
    }

    private static void inspectSharedTypePlacement(Path repository, Path path, List<String> violations) {
        try {
            String source = Files.readString(path);
            if (!DC3_PACKAGE.matcher(source).find()) {
                return;
            }

            Matcher declaration = TOP_LEVEL_SHARED_TYPE.matcher(source);
            while (declaration.find()) {
                String relative = normalized(repository.relativize(path));
                if (!relative.startsWith(COMMON_CONSTANT_SOURCE)) {
                    long line = source.substring(0, declaration.start()).lines().count() + 1;
                    violations.add("%s:%d declares %s outside dc3-common-constant"
                            .formatted(relative, line, declaration.group()));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to inspect " + path, e);
        }
    }

    private static void assertInternalDependencies(Path pom, Set<String> allowed) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);

        Element project = factory.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
        Element dependencyContainer = directChildElement(project, "dependencies");
        Set<String> internal = new HashSet<>();
        if (dependencyContainer != null) {
            NodeList dependencies = dependencyContainer.getChildNodes();
            for (int index = 0; index < dependencies.getLength(); index++) {
                Node node = dependencies.item(index);
                if (node instanceof Element dependency && "dependency".equals(dependency.getTagName())
                        && "io.github.pnoker".equals(directChildText(dependency, "groupId"))) {
                    internal.add(directChildText(dependency, "artifactId"));
                }
            }
        }

        Set<String> unexpected = new HashSet<>(internal);
        unexpected.removeAll(allowed);
        assertThat(unexpected)
                .as("Unexpected internal dependencies in %s; allowed dependency floor is %s", pom, allowed)
                .isEmpty();
    }

    private static String directChildText(Element parent, String name) {
        Element child = directChildElement(parent, name);
        return child == null ? "" : child.getTextContent().strip();
    }

    private static Element directChildElement(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element element && name.equals(element.getTagName())) {
                return element;
            }
        }
        return null;
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

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }

}
