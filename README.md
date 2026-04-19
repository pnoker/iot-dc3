<p align="right">
  <a href="./README.md">English</a> | <a href="./README.zh.md">中文</a> | <a href="./README.ja.md">日本語</a> | <a href="./README.vi.md">Tiếng Việt</a>
</p>

<p align="center">
	<img src="dc3/images/logo-blue.png" width="400" alt="IoT DC3 Logo">
<br>
<a href='https://gitee.com/pnoker/iot-dc3/stargazers'>
    <img src='https://gitee.com/pnoker/iot-dc3/badge/star.svg?theme=gvp' alt='star'/>
</a>
<a href='https://gitee.com/pnoker/iot-dc3/members'>
    <img src='https://gitee.com/pnoker/iot-dc3/badge/fork.svg?theme=gvp' alt='fork'/>
</a>
<br>
<strong>
IoT DC3 is a fully open-source, distributed Internet of Things (IoT) platform built on Spring Cloud.
It accelerates IoT solution delivery and simplifies full-lifecycle device management with a comprehensive architecture for robust, production-ready IoT systems.
It is AI-ready, enabling seamless integration of intelligent connectivity, automation, and data-driven operations.
All components and code are open-source, ensuring transparency, flexibility, and community-driven innovation.
</strong>
</p>

---

![iot-dc3-architecture](dc3/images/architecture-en.png)

# 1 Architecture

The architecture is designed for end-to-end IoT capabilities across device connectivity, data services, operational management, and extensible application integration.

- **Driver Layer**: Provides SDKs for rapid driver development and seamless connectivity to physical devices through
  standard or proprietary protocols. This layer handles southbound data acquisition and command execution;
- **Data Layer**: Supports reliable collection, storage, and retrieval of device data, exposing robust interfaces for
  real-time and historical data services;
- **Management Layer**: Serves as the core hub for distributed microservice collaboration, including service
  registration, device/driver management, command orchestration, and centralized configuration governance;
- **Application Layer**: Enables data openness, scheduling, alarms, messaging, logging, third-party integrations, and
  AI-enhanced automation scenarios.

# 2 Objectives

- **Scalability**: Supports horizontal scaling with Spring Cloud for distributed, high-throughput IoT workloads;
- **Resilience**: Minimizes single-point-of-failure risk with interchangeable service nodes and fault-tolerant design;
- **Performance**: Handles large-scale device access and telemetry workloads for demanding IoT scenarios;
- **Extensibility**: Accelerates integration of new protocols and custom drivers through SDK and service registration;
- **Deployment Flexibility**: Runs across private cloud, public cloud, and edge environments with Java compatibility;
- **Operational Efficiency**: Streamlines onboarding, registration, and permission validation for devices and services;
- **Security and Multi-Tenancy**: Enforces encrypted communication, namespace isolation, and tenant-level separation;
- **Cloud-Native Delivery**: Optimized for Kubernetes and containerized with Docker for consistent deployments;
- **AI-Ready Evolution**: Enables integration of intelligent automation and data-driven operational workflows.

# 3 Development

## 3.1 Startup Dependencies

> Choose one
>
> If you need a database SQL script, connect directly to the started database in the container for export

```bash
# Global access with standard container registry service
podman compose -f dc3/docker-compose-db.yml up -d

# Optimized registry service for users in mainland China
podman compose -f dc3/docker-compose-db-aliyun.yml up -d
```

## 3.2 Preparation

```bash
source dc3/env/dev.env.sh
mvn -s .mvn/settings.xml clean package
```

## 3.3 Start Services

> Start in order

```bash
# Gateway
java -jar dc3-gateway/target/dc3-gateway.jar

# Auth Center
java -jar dc3-center/dc3-center-auth/target/dc3-center-auth.jar

# Data Center
java -jar dc3-center/dc3-center-data/target/dc3-center-data.jar

# Manager Center
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar

# Virtual Driver
java -jar dc3-driver/dc3-driver-virtual/target/dc3-driver-virtual.jar

# Other driver: Listening Virtual Driver, Modbus TCP Driver, MQTT Driver, OPC DA Driver, OPC UA Driver, Siemens S7 Driver
```

# 4 Technology Stack

- [Java 21](https://www.java.com)
- [Spring Boot 3.5.5](https://spring.io/projects/spring-boot)
- [Spring Cloud 2025.0.0](https://spring.io/projects/spring-cloud)

# 5 Contribution

- **Branch Creation**: Start by creating a new branch from the `main` branch. Ensure that the `main` branch is
  up-to-date before branching out;
- **Branch Naming**: Follow the naming convention for the new branch: `feature/your_name/feature_description`. For
  example: `feature/pnoker/mqtt_driver`;
- **Code and Documentation**: Make your changes to the code or documentation on the new branch. Once done, commit your
  changes;
- **Pull Request**: Submit a `Pull Request` (PR) to merge your changes into the `develop` branch. Your PR will be
  reviewed and merged by the maintainers.

# 6 License

The `IoT DC3` open-source platform is licensed under the [AGPL 3.0 License](./LICENSE-AGPL.txt). 
