# IoT DC3 Documentation

> Most documentation has migrated to the project documentation site. This directory keeps
> only the source files the release process depends on.

## Online documentation

- Documentation site: <https://docs.dc3.site/>
- Documentation source repository: [pnoker/iot-dc3-docs](https://github.com/pnoker/iot-dc3-docs)

## Files retained in this directory (referenced directly by CI - do not migrate)

| File                          | Purpose                                                                            |
|-------------------------------|------------------------------------------------------------------------------------|
| [`TITLE.md`](./TITLE.md)      | Release title and summary, assembled into the GitHub Release body by `.github/workflows/docker-ci.yml` |
| [`CHANGE.md`](./CHANGE.md)    | Version changelog, written by `dc3/bin/changelog.py` and assembled into the Release body by docker-ci |
| [`RELEASE-FOOTER.md`](./RELEASE-FOOTER.md) | Release quick-start footer (docs links and image pinning), assembled into the Release body by docker-ci |
| [`USAGE.md`](./USAGE.md)      | Image usage guide; embedded into the docs site usage page and referenced from the release footer |
| [`DEPLOYMENT.md`](./DEPLOYMENT.md) | Full deployment runbook (compose scale / swarm / k8s / helm); linked from `USAGE.md` and snapshotted into the docs site by `scripts/sync-external.sh` in the docs repository |

The documentation site embeds the [changelog](https://docs.dc3.site/en/development/changelog)
and [images & deployment](https://docs.dc3.site/en/guide/usage) pages from the files above
through VitePress `<!--@include:-->` syntax, keeping a single source of truth.

## Old paths migrated to the documentation site

| Old path                    | New location                                                              |
|------------------------------|---------------------------------------------------------------------------|
| `dc3/doc/QUICKSTART.md`      | [Quick Start](https://docs.dc3.site/en/quickstart/)                       |
| `dc3/doc/ENVIRONMENT.md`     | [Environment Variables](https://docs.dc3.site/en/quickstart/environment)  |
| `dc3/doc/LOGGING.md`         | [Logging Conventions](https://docs.dc3.site/en/guide/logging)             |
| `dc3/doc/TROUBLESHOOTING.md` | [Troubleshooting](https://docs.dc3.site/en/guide/troubleshooting)         |
| `dc3/doc/MODULES.md`         | [Modules & Dependencies](https://docs.dc3.site/en/architecture/modules)   |
| `dc3/doc/DRIVER-AUTHORING.md`| [Driver Authoring](https://docs.dc3.site/en/development/driver-authoring)|
| `dc3/doc/TESTING.md`         | [Testing](https://docs.dc3.site/en/development/testing)                   |

If an external link brought you here, please update your bookmark to the new address.
