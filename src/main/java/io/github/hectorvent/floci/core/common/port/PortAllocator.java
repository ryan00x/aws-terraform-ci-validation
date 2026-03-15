package io.github.hectorvent.floci.core.common.port;

import io.github.hectorvent.floci.config.EmulatorConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe sequential port dispenser for Lambda Runtime API servers.
 */
@ApplicationScoped
public class PortAllocator {

    private final AtomicInteger counter;
    private final int maxPort;

    @Inject
    public PortAllocator(EmulatorConfig config) {
        this.counter = new AtomicInteger(config.services().lambda().runtimeApiBasePort());
        this.maxPort = config.services().lambda().runtimeApiMaxPort();
    }

    PortAllocator(int basePort, int maxPort) {
        this.counter = new AtomicInteger(basePort);
        this.maxPort = maxPort;
    }

    public int allocate() {
        int port = counter.getAndIncrement();
        if (port > maxPort) {
            counter.set(counter.get() - (maxPort - counter.get() + 1));
            port = counter.getAndIncrement();
        }
        return port;
    }
}
