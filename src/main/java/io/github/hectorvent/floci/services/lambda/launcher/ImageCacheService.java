package io.github.hectorvent.floci.services.lambda.launcher;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageResultCallback;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Ensures each Docker image is pulled only once.
 * Thread-safe using ConcurrentHashMap for double-checked locking per image.
 */
@ApplicationScoped
public class ImageCacheService {

    private static final Logger LOG = Logger.getLogger(ImageCacheService.class);

    private final DockerClient dockerClient;
    private final Set<String> pulledImages = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    @Inject
    public ImageCacheService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public void ensureImageExists(String imageUri) {
        if (pulledImages.contains(imageUri)) {
            return;
        }
        Object lock = locks.computeIfAbsent(imageUri, k -> new Object());
        synchronized (lock) {
            if (pulledImages.contains(imageUri)) {
                return;
            }
            LOG.infov("Pulling image: {0}", imageUri);
            try {
                dockerClient.pullImageCmd(imageUri)
                        .exec(new PullImageResultCallback())
                        .awaitCompletion(5, TimeUnit.MINUTES);
                pulledImages.add(imageUri);
                LOG.infov("Image pulled successfully: {0}", imageUri);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while pulling image: " + imageUri, e);
            }
        }
    }
}
