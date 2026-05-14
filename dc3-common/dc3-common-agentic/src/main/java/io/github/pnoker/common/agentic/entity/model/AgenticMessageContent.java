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
package io.github.pnoker.common.agentic.entity.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Structured content persisted for an agentic chat message.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2026.5.10
 */
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgenticMessageContent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String text;

    private String format;

    private List<Long> attachments;

    private List<String> skills;

    private List<String> tools;

    private List<Trace> traces;

    private List<Context> contexts;

    private Tokens tokens;

    private Boolean reasoning;

    private Boolean directContextProvided;

    public static AgenticMessageContent ofText(String text) {
        AgenticMessageContent content = new AgenticMessageContent();
        content.setText(text);
        return content;
    }

    @Getter
    @Setter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Trace implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String type;

        private String title;

        private String detail;

        private String name;

        private Long created;

        public static Trace of(String type, String title, String detail, String name, Long created) {
            Trace trace = new Trace();
            trace.setType(type);
            trace.setTitle(title);
            trace.setDetail(detail);
            trace.setName(name);
            trace.setCreated(created);
            return trace;
        }

    }

    @Getter
    @Setter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Context implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String type;

        private String content;

        public static Context of(String type, String content) {
            Context context = new Context();
            context.setType(type);
            context.setContent(content);
            return context;
        }

    }

    @Getter
    @Setter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Tokens implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Integer input;

        private Integer output;

        private Integer text;

        private Integer context;

        private Integer system;

        private Integer memory;

        public static Tokens of(Integer input, Integer output, Integer text, Integer context, Integer system,
                                Integer memory) {
            Tokens tokens = new Tokens();
            tokens.setInput(input);
            tokens.setOutput(output);
            tokens.setText(text);
            tokens.setContext(context);
            tokens.setSystem(system);
            tokens.setMemory(memory);
            return tokens;
        }

    }
}
