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
package io.github.pnoker.common.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

/**
 * Turbo filter dropping the hibernate-validator HV000271 deprecation notice.
 * <p>
 * The notice fires three times per gateway boot because spring-cloud-gateway
 * 4.3.0's GatewayProperties still annotates List fields with container-level
 * {@code @Valid}. Logger-level suppression is unreliable here: spring boot's
 * logging re-initialization installs a second LoggerContext that resets levels
 * assigned from this project's logback.xml, so the notice resurfaces through
 * the other context. A turbo filter attached to every context that loads this
 * configuration drops the exact message regardless of logger wiring; remove
 * this filter once spring-cloud-gateway migrates to type-argument annotations.
 *
 * @author pnoker
 * @since 2026.9.18
 */
public class Hv000271TurboFilter extends TurboFilter {

    private static final String NEEDLE = "HV000271";

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (format == null) {
            return FilterReply.NEUTRAL;
        }
        return format.contains(NEEDLE) ? FilterReply.DENY : FilterReply.NEUTRAL;
    }
}
