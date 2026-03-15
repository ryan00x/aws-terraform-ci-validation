package io.github.hectorvent.floci.services.lambda.launcher;

import io.github.hectorvent.floci.config.EmulatorConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Detects the hostname that Lambda containers should use to reach the Floci host.
 * Different on Linux native Docker vs Docker Desktop (macOS/Windows).
 */
@ApplicationScoped
public class DockerHostResolver {

    private static final Logger LOG = Logger.getLogger(DockerHostResolver.class);

    private static final String DOCKER_ENV_MARKER = "/.dockerenv";
    private static final String CGROUP_FILE = "/proc/1/cgroup";
    private static final String HOST_DOCKER_INTERNAL = "host.docker.internal";
    private static final String LINUX_DOCKER_BRIDGE = "172.17.0.1";

    private final EmulatorConfig config;

    @Inject
    public DockerHostResolver(EmulatorConfig config) {
        this.config = config;
    }

    public String resolve() {
        java.util.Optional<String> override = config.services().lambda().dockerHostOverride();
        if (override.isPresent() && !override.get().isBlank()) {
            LOG.debugv("Using configured docker host override: {0}", override.get());
            return override.get();
        }

        boolean runningInDocker = Files.exists(Path.of(DOCKER_ENV_MARKER))
                || isRunningInContainer();

        if (runningInDocker) {
            // Use this container's own IP so Lambda containers on the same network
            // can reach the Runtime API server bound to all interfaces inside this container.
            try {
                String ip = InetAddress.getLocalHost().getHostAddress();
                LOG.infov("Running in Docker — using container IP for Runtime API: {0}", ip);
                return ip;
            } catch (Exception e) {
                LOG.warnv("Could not resolve local host address, falling back to bridge IP: {0}", e.getMessage());
                return LINUX_DOCKER_BRIDGE;
            }
        }

        LOG.debugv("Using host.docker.internal for container-to-host communication");
        return HOST_DOCKER_INTERNAL;
    }

    private boolean isRunningInContainer() {
        Path cgroup = Path.of(CGROUP_FILE);
        if (!Files.exists(cgroup)) {
            return false;
        }
        try {
            String content = Files.readString(cgroup);
            return content.contains("docker") || content.contains("kubepods");
        } catch (Exception e) {
            return false;
        }
    }
}
