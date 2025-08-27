### 📦 Usage

#### 🛠️ Runtime Requirements

> These are the core requirements needed to build and run the application. Make sure to have these tools installed and properly configured in your development environment.

- **JDK 21**: Java Development Kit version 21 or higher
- **Maven**: Build automation and dependency management tool
- **Docker**: Container platform for building and running applications

#### 🚀 Quick Start

> Choose one of the following container registries:

##### 🐳 Docker Hub

> Global access with standard Docker registry service

```bash
cd iot-dc3/dc3
docker-compose -f docker-compose-db.yml up -d
docker-compose -f docker-compose.yml up -d
```

##### 🐱 Aliyun Container Registry

> Optimized registry service for users in mainland China

```bash
cd iot-dc3/dc3
docker-compose -f docker-compose-db-aliyun.yml up -d
docker-compose -f docker-compose-aliyun.yml up -d
```

#### 🏗️ Platform Support

All images are built for multiple platforms:

- `linux/amd64` - For Intel/AMD 64-bit systems
- `linux/arm64` - For ARM 64-bit systems (Apple Silicon, ARM servers)

#### 🏷️ Version Tags

- `${{ steps.variables.outputs.service_version }}` - Specific version (recommended for production)
- `latest` - Latest stable version (may change)
