/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.driver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Siemens S7 PLC Driver Application.
 * This Spring Boot application serves as a driver for communicating with Siemens S7 PLCs,
 * enabling data exchange and device control through the DC3 IoT platform.
 * The driver supports reading and writing operations to S7 PLC devices using the S7 protocol.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@SpringBootApplication
public class PlcS7DriverApplication {

    /**
     * Main method to start the Siemens S7 PLC Driver Application.
     * Initializes and launches the Spring Boot application context.
     *
     * @param args Command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(PlcS7DriverApplication.class, args);
    }
}

