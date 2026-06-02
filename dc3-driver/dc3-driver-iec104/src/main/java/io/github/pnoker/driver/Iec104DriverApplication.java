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
 * IEC 104 Driver Application serves as the entry point for the DC3 IEC 60870-5-104
 * Driver module. This driver implements the IEC 104 protocol for communication with
 * substation automation and telecontrol equipment.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@SpringBootApplication
public class Iec104DriverApplication {

    /**
     * IEC 104 Driver Application Main Method Start the Spring Boot application by
     * passing the Iec104DriverApplication class and command line arguments
     *
     * @param args Command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(Iec104DriverApplication.class, args);
    }

}
