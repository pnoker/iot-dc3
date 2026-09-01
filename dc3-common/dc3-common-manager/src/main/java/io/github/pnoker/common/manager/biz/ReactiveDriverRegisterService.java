package io.github.pnoker.common.manager.biz;

import io.github.pnoker.api.common.driver.GrpcDriverRegisterDTO;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.EventAttributeBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import reactor.core.publisher.Mono;

import java.util.List;

/** Reactive driver registration port used by the driver runtime gRPC API. */
public interface ReactiveDriverRegisterService {

    Mono<Registration> register(GrpcDriverRegisterDTO request);

    record Registration(DriverBO driver, List<DriverAttributeBO> driverAttributes,
                       List<PointAttributeBO> pointAttributes, List<CommandAttributeBO> commandAttributes,
                       List<EventAttributeBO> eventAttributes) {
        public Registration {
            driverAttributes = List.copyOf(driverAttributes);
            pointAttributes = List.copyOf(pointAttributes);
            commandAttributes = List.copyOf(commandAttributes);
            eventAttributes = List.copyOf(eventAttributes);
        }
    }
}
