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
package io.github.pnoker.common.agentic.skill;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Scans the classpath for skill definition YAML files ({@code classpath*:skills/*.yml})
 * at application startup, parses each file into a {@link SkillDefinition}, and registers
 * enabled skills into the {@link SkillRegistry}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class SkillLoader implements ApplicationRunner {

    private final SkillRegistry registry;

    public SkillLoader(SkillRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:skills/*.yml");

        List<String> loaded = new ArrayList<>();

        Yaml yaml = new Yaml();
        for (Resource resource : resources) {
            try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                Map<String, Object> map = yaml.load(reader);
                SkillDefinition skill = mapToSkill(map);
                if (skill.isEnabled()) {
                    registry.register(skill);
                    loaded.add(skill.getName());
                } else {
                    log.debug("Agentic skill skipped, skill={}, reason=disabled", skill.getName());
                }
            } catch (Exception e) {
                log.error("Agentic skill load failed, resource={}", resource.getDescription(), e);
            }
        }

        log.info("Agentic skills loaded, count={}, skills={}", loaded.size(), loaded);
    }

    @SuppressWarnings("unchecked")
    private SkillDefinition mapToSkill(Map<String, Object> map) {
        SkillDefinition skill = new SkillDefinition();
        skill.setName((String) map.get("name"));
        skill.setDescription((String) map.get("description"));
        skill.setEnabled(Boolean.TRUE.equals(map.get("enabled")));
        skill.setSystemPromptAddition((String) map.get("system-prompt-addition"));
        skill.setTools((List<String>) map.get("tools"));

        List<Map<String, String>> exampleMaps = (List<Map<String, String>>) map.get("examples");
        if (Objects.nonNull(exampleMaps)) {
            List<SkillDefinition.SkillExample> examples = new ArrayList<>();
            for (Map<String, String> ex : exampleMaps) {
                examples.add(new SkillDefinition.SkillExample(ex.get("user"), ex.get("assistant")));
            }
            skill.setExamples(examples);
        }

        return skill;
    }

}
