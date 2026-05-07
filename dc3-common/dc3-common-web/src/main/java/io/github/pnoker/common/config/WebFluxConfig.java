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

package io.github.pnoker.common.config;

import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * WebFlux Configuration Class
 * <p>
 * Configuration class for Spring WebFlux reactive web framework. Configures resource
 * handlers for static files and custom JSON message codecs using the project's JsonUtil
 * for consistent serialization.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@EnableWebFlux
@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

	/**
	 * Configure static resource handlers
	 * @param registry ResourceHandlerRegistry for configuring static resources
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/static/");
	}

	/**
	 * Configure HTTP message codecs for JSON serialization/deserialization
	 * @param configurer ServerCodecConfigurer for configuring message codecs
	 */
	@Override
	public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
		configurer.customCodecs().registerWithDefaultConfig(new JacksonJsonEncoder(JsonUtil.getJsonMapper()));
		configurer.customCodecs().registerWithDefaultConfig(new JacksonJsonDecoder(JsonUtil.getJsonMapper()));
	}

}
