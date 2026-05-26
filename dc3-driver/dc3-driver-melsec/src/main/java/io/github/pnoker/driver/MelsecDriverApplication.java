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
 * Main application class for the Mitsubishi Melsec MC driver.
 * <p>
 * This driver enables communication with Mitsubishi PLCs (FX5U, L series,
 * Q series, QnA series, A series, iQ-R series) using the Melsec (MC)
 * communication protocol. It supports 1E, 3E, and 4E frame types with
 * thread-safe connection management.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2016.10.1
 */
@SpringBootApplication
public class MelsecDriverApplication {

    /**
     * Main entry point for the Mitsubishi Melsec MC driver application.
     *
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(MelsecDriverApplication.class, args);
    }

}
