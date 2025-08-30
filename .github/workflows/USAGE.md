## 📦 Usage

### 🎮 Requirements

> These are the core requirements needed to build and run the application. Make sure to have these tools installed and properly configured in your development environment.

- **JDK 21**: Java Development Kit version 21 or higher
- **Maven**: Build automation and dependency management tool
- **Docker**: Container platform for building and running applications

### 🚀 Quick Start

> Choose one of the following container registries:

#### 🦁 Docker Hub

> Global access with standard Docker registry service

```bash
cd iot-dc3/dc3
docker-compose -f docker-compose-db.yml up -d
docker-compose -f docker-compose.yml up -d
```

#### 🐱 Aliyun Container Registry

> Optimized registry service for users in mainland China

```bash
cd iot-dc3/dc3
docker-compose -f docker-compose-db-aliyun.yml up -d
docker-compose -f docker-compose-aliyun.yml up -d
```

## 🐳 Container

### ⛳ Platform Support

All images are built for multiple platforms:

- `linux/amd64` - For Intel/AMD 64-bit systems
- `linux/arm64` - For ARM 64-bit systems (Apple Silicon, ARM servers)

### 🏷️ Version Tags

- `${{ steps.variables.outputs.service_version }}` - Specific version (recommended for production)
- `latest` - Latest stable version (may change)

### 🛞 Images

| Docker Hub                                                                                                                                                                   | Aliyun Container Registry                                                                                                                                                   |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `${{ steps.variables.outputs.docker_registry_default }}/${{ steps.variables.outputs.service_name_gateway }}:${{ steps.variables.outputs.service_version }}`                  | `${{ steps.variables.outputsdocker_registry_aliyun }}/${{ steps.variables.outputs.service_name_gateway }}:${{ steps.variables.outputs.service_version }}`                   |
| `${{ steps.variables.outputs.docker_registry_default }}/${{ steps.variables.outputs.service_name_center_auth }}:${{ steps.variables.outputs.service_version }}`              | `${{ steps.variables.outputs.docker_registry_aliyun }}/${{ steps.variables.outputs.service_name_center_auth }}:${{ steps.variables.outputs.service_version }}`              |
| `${{ steps.variables.outputs.docker_registry_default }}/${{ steps.variables.outputs.service_name_center_data }}:${{ steps.variables.outputs.service_version }}`              | `${{ steps.variables.outputs.docker_registry_aliyun }}/${{ steps.variables.outputs.service_name_center_data }}:${{ steps.variables.outputs.service_version }}`              |
| `${{ steps.variables.outputs.docker_registry_default }}/${{ steps.variables.outputs.service_name_center_manager }}:${{ steps.variables.outputs.service_version }}`           | `${{ steps.variables.outputs.docker_registry_aliyun }}/${{ steps.variables.outputs.service_name_center_manager }}:${{ steps.variables.outputs.service_version }}`           |
| `${{ steps.variables.outputs.docker_registry_default }}/${{ steps.variables.outputs.service_name_center_single }}:${{ steps.variables.outputs.service_version }}`            | `${{ steps.variables.outputs.docker_registry_aliyun }}/${{ steps.variables.outputs.service_name_center_single }}:${{ steps.variables.outputs.service_version }}`            |
| `${{ steps.variables.outputs.docker_registry_default }}/${{ steps.variables.outputs.service_name_driver_listening_virtual }}:${{ steps.variables.outputs.service_version }}` | `${{ steps.variables.outputs.docker_registry_aliyun }}/${{ steps.variables.outputs.service_name_driver_listening_virtual }}:${{ steps.variables.outputs.service_version }}` |
| `${{ steps.variables.outputs.docker_registry_default }}/${{ steps.variables.outputs.service_name_driver_modbus_tcp }}:${{ steps.variables.outputs.service_version }}`        | `${{ steps.variables.outputs.docker_registry_aliyun }}/${{ steps.variables.outputs.service_name_driver_modbus_tcp }}:${{ steps.variables.outputs.service_version }}`        |
| `${{ steps.variables.outputs.docker_registry_default }}/${{ steps.variables.outputs.service_name_driver_mqtt }}:${{ steps.variables.outputs.service_version }}`              | `${{ steps.variables.outputs.docker_registry_aliyun }}/${{ steps.variables.outputs.service_name_driver_mqtt }}:${{ steps.variables.outputs.service_version }}`              |
| `${{ steps.variables.outputs.docker_registry_default }}/${{ steps.variables.outputs.service_name_driver_opc_da }}:${{ steps.variables.outputs.service_version }}`            | `${{ steps.variables.outputs.docker_registry_aliyun }}/${{ steps.variables.outputs.service_name_driver_opc_da }}:${{ steps.variables.outputs.service_version }}`            |
| `${{ steps.variables.outputs.docker_registry_default }}/${{ steps.variables.outputs.service_name_driver_opc_ua }}:${{ steps.variables.outputs.service_version }}`            | `${{ steps.variables.outputs.docker_registry_aliyun }}/${{ steps.variables.outputs.service_name_driver_opc_ua }}:${{ steps.variables.outputs.service_version }}`            |
| `${{ steps.variables.outputs.docker_registry_default }}/${{ steps.variables.outputs.service_name_driver_plcs7 }}:${{ steps.variables.outputs.service_version }}`             | `${{ steps.variables.outputs.docker_registry_aliyun }}/${{ steps.variables.outputs.service_name_driver_plcs7 }}:${{ steps.variables.outputs.service_version }}`             |
| `${{ steps.variables.outputs.docker_registry_default }}/${{ steps.variables.outputs.service_name_driver_virtual }}:${{ steps.variables.outputs.service_version }}`           | `${{ steps.variables.outputs.docker_registry_aliyun }}/${{ steps.variables.outputs.service_name_driver_virtual }}:${{ steps.variables.outputs.service_version }}`           |
|                                                                                                                                                                              |                                                                                                                                                                             |
