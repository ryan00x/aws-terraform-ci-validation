package io.github.hectorvent.floci.services.lambda;

import io.github.hectorvent.floci.core.storage.StorageBackend;
import io.github.hectorvent.floci.core.storage.StorageFactory;
import io.github.hectorvent.floci.services.lambda.model.EventSourceMapping;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

/**
 * Wraps the storage backend for Lambda Event Source Mappings, keyed by UUID.
 */
@ApplicationScoped
public class EsmStore {

    private final StorageBackend<String, EventSourceMapping> backend;

    @Inject
    public EsmStore(StorageFactory storageFactory) {
        this.backend = storageFactory.create("lambda", "lambda-esm.json",
                new TypeReference<>() {
                });
    }

    EsmStore(StorageBackend<String, EventSourceMapping> backend) {
        this.backend = backend;
    }

    public void save(EventSourceMapping esm) {
        backend.put(esm.getUuid(), esm);
    }

    public Optional<EventSourceMapping> get(String uuid) {
        return backend.get(uuid);
    }

    public List<EventSourceMapping> list() {
        return backend.scan(k -> true);
    }

    public List<EventSourceMapping> listByFunction(String functionKey) {
        return backend.scan(k -> {
            var esm = backend.get(k).orElse(null);
            if (esm == null) return false;
            // Match by full ARN or by short function name
            return functionKey.equals(esm.getFunctionArn()) || functionKey.equals(esm.getFunctionName());
        });
    }

    public void delete(String uuid) {
        backend.delete(uuid);
    }
}
