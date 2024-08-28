package io.github.ponker.center.ekuiper.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PortableDataVO {
    private String name;
    private String describe;
    private String version;
    private String language;
    private String executable;
    private String[] sources;
    private String[] sinks;
    private String[] functions;
    public PortableDataVO(String name, String version, String language, String executable, String[] sources, String[] sinks, String[] functions) {
        this.name = name;
        this.version = version;
        this.language = language;
        this.executable = executable;
        this.sources = sources;
        this.sinks = sinks;
        this.functions = functions;
    }
}
