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
 * Main application class for the Siemens S7 PLC driver.
 * <p>
 * This driver enables communication with Siemens S7 PLCs (S7-200, S7-300, S7-400,
 * S7-1200, S7-1500) using the S7 communication protocol. It provides read/write access to
 * data blocks (DB), memory areas, and I/O points with thread-safe connection management.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@SpringBootApplication
public class PlcS7DriverApplication {

	/**
	 * Main entry point for the Siemens S7 PLC driver application.
	 * @param args command line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(PlcS7DriverApplication.class, args);
	}

}
