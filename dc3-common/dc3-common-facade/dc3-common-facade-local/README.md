# DC3 Common Facade Local

`dc3-common-facade-local` is a dependency-only POM that aggregates every in-process facade implementation:

- `dc3-common-facade-local-auth`
- `dc3-common-facade-local-data`
- `dc3-common-facade-local-manager`

It contains no Java implementation sources. Use it for an all-in-one process that deliberately needs the complete local
surface. Other applications should depend on the narrowest domain-specific local facade to avoid unrelated transitive
dependencies.

## Usage

```xml
<dependency>
    <groupId>io.github.pnoker</groupId>
    <artifactId>dc3-common-facade-local</artifactId>
</dependency>
```

## Verification

Verify the aggregate and its dependencies from the repository root:

```bash
mvn -s .mvn/settings.xml -q \
  -pl dc3-common/dc3-common-facade/dc3-common-facade-local -am \
  -DskipTests compile
```
