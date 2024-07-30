package io.github.ponker.center.ekuiper.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PortableDataVO {
    private String name;
    private String version;
    private String language;
    private String executable;
    private String[] sources;
    private String[] sinks;
    private String[] functions;
}
