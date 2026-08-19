## 🚀 Quick Start

```bash
# Docker Hub (global)
cd iot-dc3
make up-db
make up STACK=app

# China registry (mainland mirror)
make up-db-cn
make up STACK=app REGISTRY=cn
```

Pin this release by tag, e.g. `pnoker/dc3-gateway:${SERVICE_VERSION}` - see the full multi-arch image table
and the standard usage guide at **<https://docs.dc3.site/en/guide/usage>**.

Scaling out (compose scale / Docker Swarm / Kubernetes / Helm) is covered in the
**[Deployment Guide](https://docs.dc3.site/en/guide/deployment)**; environment variables in
**[Environment Reference](https://docs.dc3.site/en/quickstart/environment)**. Documentation home:
**<https://docs.dc3.site>**.
