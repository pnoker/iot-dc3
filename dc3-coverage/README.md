# DC3 Coverage

`dc3-coverage` is a report-only Maven module. It aggregates JaCoCo execution data from the covered handwritten-code
modules and applies the repository's absolute coverage gate.

## Generate and Verify the Report

From the repository root:

```bash
make coverage
```

Equivalent Maven command:

```bash
mvn -s .mvn/settings.xml -B -Dmaven.test.skip=false -pl dc3-coverage -am verify
```

Outputs:

- HTML: `dc3-coverage/target/site/jacoco-aggregate/index.html`
- XML: `dc3-coverage/target/site/jacoco-aggregate/jacoco.xml`

## Gate

The minimum line and branch ratios are configured in this module's `pom.xml`. The verify phase passes the aggregate XML
to `scripts/check_coverage.py`, which checks those absolute thresholds.

There is currently no baseline-relative regression calculation. Do not describe the build as enforcing a percentage
drop against a previous commit unless such a comparison is implemented.

Use `-Dcoverage.check.skip=true` only for local diagnosis; do not disable the gate in normal CI or release validation.

## Adding covered modules

Add a dependency in `pom.xml` only for a module whose handwritten classes should contribute to the aggregate. Generated
API artifacts and deployment-only wrappers should remain excluded unless the coverage policy changes deliberately.
