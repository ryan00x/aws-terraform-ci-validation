package io.github.hectorvent.floci.lifecycle;

import io.github.hectorvent.floci.config.EmulatorConfig;
import io.github.hectorvent.floci.core.common.ServiceRegistry;
import io.github.hectorvent.floci.core.storage.StorageFactory;
import io.github.hectorvent.floci.services.elasticache.proxy.ElastiCacheProxyManager;
import io.github.hectorvent.floci.services.rds.container.RdsContainerManager;
import io.github.hectorvent.floci.services.rds.proxy.RdsProxyManager;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EmulatorLifecycle {

    private static final Logger LOG = Logger.getLogger(EmulatorLifecycle.class);

    private final StorageFactory storageFactory;
    private final ServiceRegistry serviceRegistry;
    private final EmulatorConfig config;
    private final ElastiCacheProxyManager elastiCacheProxyManager;
    private final RdsProxyManager rdsProxyManager;
    private final RdsContainerManager rdsContainerManager;

    @Inject
    public EmulatorLifecycle(StorageFactory storageFactory, ServiceRegistry serviceRegistry,
                             EmulatorConfig config, ElastiCacheProxyManager elastiCacheProxyManager,
                             RdsProxyManager rdsProxyManager, RdsContainerManager rdsContainerManager) {
        this.storageFactory = storageFactory;
        this.serviceRegistry = serviceRegistry;
        this.config = config;
        this.elastiCacheProxyManager = elastiCacheProxyManager;
        this.rdsProxyManager = rdsProxyManager;
        this.rdsContainerManager = rdsContainerManager;
    }

    void onStart(@Observes StartupEvent event) {
        LOG.info("=== AWS Local Emulator Starting ===");
        LOG.infov("Storage mode: {0}", config.storage().mode());
        LOG.infov("Persistent path: {0}", config.storage().persistentPath());
        serviceRegistry.logEnabledServices();

        storageFactory.loadAll();
        LOG.info("=== AWS Local Emulator Ready ===");
    }

    void onStop(@Observes ShutdownEvent event) {
        LOG.info("=== AWS Local Emulator Shutting Down ===");
        elastiCacheProxyManager.stopAll();
        rdsProxyManager.stopAll();
        storageFactory.shutdownAll();
        LOG.info("=== AWS Local Emulator Stopped ===");
    }
}
