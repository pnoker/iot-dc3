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

package io.github.pnoker.common.utils;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.constant.common.TimeConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.sql.Types;

/**
 * MyBatis Utility Class
 * <p>
 * Utility class for MyBatis-Plus code generation and configuration. Provides methods to
 * configure database connections, global settings, data source settings, and strategy
 * configurations for automatic code generation.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
public class MybatisUtil {

	private MybatisUtil() {
		throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
	}

	/**
	 * Default code generator for MyBatis-Plus
	 * <p>
	 * Database connection parameters can be overridden by environment variables
	 * @param defaultHost Default database host
	 * @param defaultPort Default database port
	 * @param defaultDb Default database name
	 * @param defaultSchema Default database schema
	 * @param defaultUsername Default database username
	 * @param defaultPassword Default database password
	 * @return Configured FastAutoGenerator instance
	 */
	public static FastAutoGenerator defaultGenerator(String defaultHost, String defaultPort, String defaultDb,
			String defaultSchema, String defaultUsername, String defaultPassword) {
		String host = System.getenv("POSTGRES_HOST");
		if (StringUtils.isEmpty(host)) {
			host = defaultHost;
		}
		String port = System.getenv("POSTGRES_PORT");
		if (StringUtils.isEmpty(port)) {
			port = defaultPort;
		}
		String db = System.getenv("POSTGRES_DB");
		if (StringUtils.isEmpty(db)) {
			db = defaultDb;
		}
		String schema = System.getenv("POSTGRES_SCHEMA");
		if (StringUtils.isEmpty(schema)) {
			schema = defaultSchema;
		}
		String username = System.getenv("POSTGRES_USERNAME");
		if (StringUtils.isEmpty(username)) {
			username = defaultUsername;
		}
		String password = System.getenv("POSTGRES_PASSWORD");
		if (StringUtils.isEmpty(password)) {
			password = defaultPassword;
		}

		return FastAutoGenerator.create(
				String.format("jdbc:postgresql://%s:%s/%s?currentSchema=%s&useSSL=false", host, port, db, schema),
				username, password);
	}

	/**
	 * Default global configuration for code generation
	 * @param builder GlobalConfig.Builder instance
	 * @param path Root directory for generated files
	 */
	public static void defaultGlobalConfig(GlobalConfig.Builder builder, String path) {
		builder.outputDir(path + "/java")
			.author(DefaultConstant.USER_NAME)
			.commentDate(TimeConstant.DAY_DATE_FORMAT1)
			.disableOpenDir();
	}

	/**
	 * Default data source configuration for code generation
	 * @param builder DataSourceConfig.Builder instance
	 */
	public static void defaultDataSourceConfig(DataSourceConfig.Builder builder) {
		builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
			int typeCode = metaInfo.getJdbcType().TYPE_CODE;
			if (typeCode == Types.SMALLINT) {
				return DbColumnType.BYTE;
			}
			return typeRegistry.getColumnType(metaInfo);
		});
	}

	/**
	 * Default strategy configuration for code generation
	 * @param builder StrategyConfig.Builder instance
	 */
	public static void defaultStrategyConfig(StrategyConfig.Builder builder) {
		builder.addTablePrefix("dc3_")
			.entityBuilder()
			.idType(IdType.ASSIGN_ID)
			.enableLombok()
			.formatFileName("%sDO")
			.enableTableFieldAnnotation()
			.enableRemoveIsPrefix()
			.enableFileOverride()
			.logicDeleteColumnName("deleted")
			.controllerBuilder()
			.disable()
			.serviceBuilder()
			.formatServiceFileName("%sManager")
			.formatServiceImplFileName("%sManagerImpl")
			.enableFileOverride()
			.mapperBuilder()
			.enableFileOverride();
	}

}
