# Security Policy

> :lock: **Note:** iot-dc3 is a distributed Internet of Things (IoT) platform that involves device access, data
> collection, and command dispatch. Security issues not only affect
> system operation but may also cause data or control risks. Please pay close attention to security configuration and
> version updates.

## Supported Versions

> We usually provide security patches and updates only for the mainline versions that are currently actively maintained.

The following table lists the iot-dc3 versions that are currently supported with security updates:

| Version  | Supported          |
|----------|--------------------|
| 2025.9.x | :white_check_mark: |
| 2025.6.x | :white_check_mark: |
| 2025.x.x | :white_check_mark: |

## Reporting a Vulnerability

> We take security issues very seriously.  
> If a vulnerability is verified, we will fix it as soon as possible and disclose the fix information in the release
> notes.

If you find a potential security vulnerability while using **iot-dc3**, **do not disclose it publicly in issues or
discussion areas**, but report it through the following private
channels:

1. **Email report**:  
   Send an email to the project maintenance team, and please include the keyword `Security Vulnerability` in the subject
   line.

2. **Direct message report**:  
   You can directly contact the project maintainers through the private message function on Gitee or GitHub.

## Security Best Practices

To ensure the security and stability of the iot-dc3 platform in production environments, it is recommended to follow
these practices:

- :white_check_mark: Always use supported versions;
- :no_entry_sign: Do not expose core communication ports (such as MQTT, TCP, Modbus gateways) directly to the public
  network;
- :lock: Use secure authentication mechanisms and enable HTTPS / SSL encryption;
- :arrows_counterclockwise: Regularly update system dependencies and Docker images;
- :jigsaw: Only authorize trusted devices and users to access the system;
- :bar_chart: Apply the principle of least privilege and perform access auditing on external interfaces.
