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
<strong>IoT DC3 is an open-source distributed Internet of Things (IoT) platform based on Spring Cloud. It is used for rapid development of IoT projects and management of IoT devices, providing a comprehensive solution for IoT system development.</strong>
</p>

---

![iot-dc3-architecture](dc3/images/architecture1.jpg)

# 1Architecture

- **Driver Layer**: Provides SDKs for connecting physical devices via standard or proprietary protocols, responsible for
  southbound data collection and command control. The `SDK`
  enables rapid driver development;
- **Data Layer**: Responsible for collecting and storing device data, and providing data management interface services;
- **Management Layer**: Provides a microservice registry, device command interfaces, device registration and pairing,
  and a data management center. It is the core part of all
  microservice interactions, managing various configuration data and providing external interface services;
- **Application Layer**(partially completed): Provides data openness, task scheduling, alarm and message notifications,
  log management, and the capability to integrate with
  third-party platforms.

# 2 Target

- **Scalable**: A horizontally scalable platform built using leading open-source technology, Spring Cloud;
- **Fault Tolerant**: No single point of failure, with each node in the cluster being identical;
- **Robust and Efficient**: A single server node can handle even hundreds of thousands of devices depending on the use
  case;
- **Customizable**: Add new device protocols and register them to the service center;
- **Cross-Platform**: Compatible with Java environments, enabling distributed multi-platform deployment;
- **Autonomous and Controllable**: Supports private cloud, public cloud, and edge deployments;
- **Comprehensive**: Fast device onboarding, registration, and permission validation;
- **Secure**: Data transmission is encrypted;
- **Multi-Tenant**: Supports namespaces and multi-tenancy;
- **Cloud-Native**: Kubernetes;
- **Containerized**: Docker.

# 3  Contribution

- Check out a new branch from the `main` branch (ensure `main` branch is up-to-date);
- Naming format for the new branch: `feature/you_name/feature_description`, for example: `feature/pnoker/mqtt_driver`;
- Edit documents and code on the new branch, then commit the changes;
- Submit a `Pull Request` (PR) to merge the changes into the `develop` branch, and await review and merging by the
  author;
- Once merged, we will add your UserID to the  [Contributor](https://doc.dc3.site/contributor) list.

# 4 License

`IOT DC3` open-source platform follows the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0.html), It
permits commercial use but requires retention of author and
copyright information.