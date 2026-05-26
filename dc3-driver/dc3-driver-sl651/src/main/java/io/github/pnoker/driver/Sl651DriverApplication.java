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
 * Main application class for the SL651-2014 hydrological telemetry driver.
 * <p>
 * This driver starts an SL651 TCP server that listens for incoming telemetry
 * data from remote hydrological monitoring stations. Received data is parsed
 * and forwarded to the DC3 platform for storage and processing.
 * </p>
 * <p>
 * Note: SL651 is a server-side protocol. Unlike PLC drivers that actively
 * read/write device points, this driver receives unsolicited telemetry
 * reports from remote stations.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2016.10.1
 */
@SpringBootApplication
public class Sl651DriverApplication {

    /**
     * Main entry point for the SL651 hydrological telemetry driver application.
     *
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(Sl651DriverApplication.class, args);
    }

}
