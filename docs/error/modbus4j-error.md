> 该问题常见于配置了私有的 Maven，而导致 modbus4j 依赖无法正常下载



**该问题可以通过以下方式解决：**

> 请使用 `dc3/dependencies/maven/settings.xml` 该 Maven 配置文件

```bash
mvn -s dc3/dependencies/maven/settings.xml clean -U package
```