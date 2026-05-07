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

package io.github.pnoker.common.facade.api;

/**
 * Protocol-neutral token facade. Mirrors {@code api.center.auth.TokenApi}.
 * <p>
 * Returns a plain boolean rather than a {@code TokenBO} because callers only care whether
 * the token is valid — the existing RPC only returns an expiry string that nobody reads
 * from the gateway.
 *
 * @author pnoker
 * @since 2026.5.5
 */
public interface TokenFacade {

	/**
	 * Validate a token triple against the backing auth service.
	 * @param tenant tenant code
	 * @param name login name
	 * @param salt salt the client holds
	 * @param token token the client holds
	 * @return {@code true} when the triple is valid and unexpired
	 */
	boolean checkValid(String tenant, String name, String salt, String token);

}
