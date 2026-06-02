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
 * EtherNet/IP Driver Application serves as the entry point for the DC3 EtherNet/IP
 * Driver module. This driver implements EtherNet/IP protocol for communication with
 * Rockwell Allen-Bradley PLCs and other CIP-compatible devices.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@SpringBootApplication
public class EthernetIpDriverApplication {

    /**
     * EtherNet/IP Driver Application Main Method Start the Spring Boot application by
     * passing the EthernetIpDriverApplication class and command line arguments
     *
     * @param args Command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(EthernetIpDriverApplication.class, args);
    }

}
