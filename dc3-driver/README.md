# DC3 Drivers

`dc3-driver` contains protocol adapters built on `dc3-common-driver`. Each driver owns protocol I/O and user-facing
attribute metadata; the SDK owns registration, scheduling, metadata refresh, health integration, commands, and value
dispatch.

## Modules

| Family               | Modules                                                                                                                                                                                      |
|----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Industrial protocols | `bacnet-ip`, `can`, `dlms`, `dlt645`, `dnp3`, `ethernet-ip`, `fins`, `iec104`, `iec61850`, `knx`, `mbus`, `melsec`, `modbus-rtu`, `modbus-tcp`, `opc-da`, `opc-ua`, `plcs7`, `sl651`, `snmp` |
| Messaging/network    | `coap`, `http`, `kafka`, `lorawan`, `lwm2m`, `mqtt`, `tcp-udp`                                                                                                                               |
| Databases            | `mysql`, `oracle`, `postgresql`, `redis`, `sqlserver`                                                                                                                                        |
| Local/device buses   | `ble`, `serial`, `zigbee`                                                                                                                                                                    |
| Simulation           | `virtual`, `listening-virtual`                                                                                                                                                               |

The following drivers currently declare incomplete protocol I/O and must be treated as work in progress: `can`, `dlms`,
`ethernet-ip`, `iec104`, `lwm2m`, `mqtt`, `opc-da`, and `zigbee`. Check the child README and implementation before
production use.

## Driver metadata

`src/main/resources/application.yml` is authoritative for:

- stable driver `code` and client/server `type`;
- driver, point, command, and event attribute codes/types/defaults;
- scheduling and health defaults;
- local buffering configuration.

Changing a driver code requires a metadata and RabbitMQ routing migration. Keep display names and remarks in English.

## Build and run

```bash
mvn -s .mvn/settings.xml -q -f dc3-driver/pom.xml -DskipTests compile
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-modbus-tcp -am test
make run SERVICE=modbus-tcp
```

Replace `modbus-tcp` with the desired driver service. A runnable driver also needs database/messaging infrastructure and
the required center services; use the root Makefile to start the appropriate stack.

## Child README requirements

Every driver README should document readiness, prerequisites, the supported attribute surface, read/write operations, a
focused test command, limitations, and any host/container device requirements. Include exact codes/defaults when they
help operators, but treat `application.yml` as the authoritative metadata definition.
