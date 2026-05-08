# Storage Modes

Floci supports four storage backends. You can set a global default and override it per service.

## Modes

| Mode | Data survives restart | Write performance | Use case |
|---|---|---|---|
| `memory` | No | Fastest | Unit tests, CI pipelines |
| `persistent` | Yes | Synchronous disk write on every change | Development with durable state |
| `hybrid` | Yes | In-memory reads, async flush to disk | General local development |
| `wal` | Yes | Append-only write-ahead log with compaction | High-write workloads |

## Global Configuration

```yaml title="application.yml"
floci:
  storage:
    mode: memory              # shipped default (application.yml)
    persistent-path: ./data   # base directory for all persistent data
    wal:
      compaction-interval-ms: 30000
```

!!! note "Code default vs shipped default"
    `EmulatorConfig.StorageConfig.mode` has a Java-level `@WithDefault("hybrid")`, but the shipped `src/main/resources/application.yml` overrides it to `memory`. Running the stock Docker image gives you `memory`; if you supply your own `application.yml` and omit `storage.mode`, you fall back to the code default `hybrid`.

## Per-Service Override

When `mode` is omitted for a service, it inherits the global `storage.mode`. Only set a per-service mode when you need a different behaviour for that service.

```yaml title="application.yml"
floci:
  storage:
    mode: memory              # default for all services
    services:
      dynamodb:
        mode: persistent      # DynamoDB uses persistent; everything else uses memory
        flush-interval-ms: 5000
      s3:
        mode: hybrid          # S3 uses hybrid; everything else uses memory
```

## Per-Service Storage Overrides

Override the global mode for individual services via environment variables. When not set, the service inherits `FLOCI_STORAGE_MODE`.

| Variable                                                        | Default        | Description                            |
|-----------------------------------------------------------------|----------------|----------------------------------------|
| `FLOCI_STORAGE_SERVICES_SSM_MODE`                               | global default | SSM storage mode                       |
| `FLOCI_STORAGE_SERVICES_SSM_FLUSH_INTERVAL_MS`                  | `5000`         | SSM flush interval (ms)                |
| `FLOCI_STORAGE_SERVICES_SQS_MODE`                               | global default | SQS storage mode                       |
| `FLOCI_STORAGE_SERVICES_S3_MODE`                                | global default | S3 storage mode                        |
| `FLOCI_STORAGE_SERVICES_DYNAMODB_MODE`                          | global default | DynamoDB storage mode                  |
| `FLOCI_STORAGE_SERVICES_DYNAMODB_FLUSH_INTERVAL_MS`             | `5000`         | DynamoDB flush interval (ms)           |
| `FLOCI_STORAGE_SERVICES_SNS_MODE`                               | global default | SNS storage mode                       |
| `FLOCI_STORAGE_SERVICES_SNS_FLUSH_INTERVAL_MS`                  | `5000`         | SNS flush interval (ms)                |
| `FLOCI_STORAGE_SERVICES_LAMBDA_MODE`                            | global default | Lambda storage mode                    |
| `FLOCI_STORAGE_SERVICES_LAMBDA_FLUSH_INTERVAL_MS`               | `5000`         | Lambda flush interval (ms)             |
| `FLOCI_STORAGE_SERVICES_CLOUDWATCHLOGS_MODE`                    | global default | CloudWatch Logs storage mode           |
| `FLOCI_STORAGE_SERVICES_CLOUDWATCHLOGS_FLUSH_INTERVAL_MS`       | `5000`         | CloudWatch Logs flush interval (ms)    |
| `FLOCI_STORAGE_SERVICES_CLOUDWATCHMETRICS_MODE`                 | global default | CloudWatch Metrics storage mode        |
| `FLOCI_STORAGE_SERVICES_CLOUDWATCHMETRICS_FLUSH_INTERVAL_MS`    | `5000`         | CloudWatch Metrics flush interval (ms) |
| `FLOCI_STORAGE_SERVICES_SECRETSMANAGER_MODE`                    | global default | Secrets Manager storage mode           |
| `FLOCI_STORAGE_SERVICES_SECRETSMANAGER_FLUSH_INTERVAL_MS`       | `5000`         | Secrets Manager flush interval (ms)    |
| `FLOCI_STORAGE_SERVICES_ACM_MODE`                               | global default | ACM storage mode                       |
| `FLOCI_STORAGE_SERVICES_ACM_FLUSH_INTERVAL_MS`                  | `5000`         | ACM flush interval (ms)                |
| `FLOCI_STORAGE_SERVICES_OPENSEARCH_MODE`                        | global default | OpenSearch storage mode                |
| `FLOCI_STORAGE_SERVICES_OPENSEARCH_FLUSH_INTERVAL_MS`           | `5000`         | OpenSearch flush interval (ms)         |
| `FLOCI_STORAGE_SERVICES_RDS_MODE`                               | global default | RDS storage mode (see note below)      |

!!! note "RDS storage mode"
    `FLOCI_STORAGE_SERVICES_RDS_MODE` controls Floci's own metadata persistence for RDS, not the
    DB container volumes. In all modes, each DB instance or cluster gets a named Docker volume
    (`floci-rds-{volumeId}`). In `memory` mode the volume is automatically removed when the
    instance is deleted. In other modes the volume is retained unless
    `FLOCI_STORAGE_PRUNE_VOLUMES_ON_DELETE=true`.

## Container Storage (RDS, OpenSearch, MSK, ECR)

Services that spawn Docker containers (RDS, OpenSearch, MSK, ECR registry) need a volume for their
data. Floci manages this automatically using **named Docker volumes** — no extra configuration
required.

### How it works

Each resource gets a `volumeId` (a 6-character hex string, e.g. `a1b2c3`) generated at creation
time and stored in the resource model. The container name and volume name both use this suffix:

```
floci-rds-a1b2c3         # RDS instance container and volume
floci-opensearch-b4c5d6  # OpenSearch domain container and volume
floci-msk-e7f8a9         # MSK cluster container and volume
floci-ecr-registry-data  # ECR shared registry volume (singleton)
```

Volumes are labelled `floci=true` so you can manage them with standard Docker commands:

```bash
# List all Floci-managed volumes
docker volume ls --filter label=floci=true

# Remove all Floci-managed volumes (destructive)
docker volume prune --filter label=floci=true
```

### Volume lifecycle

By default volumes survive resource deletion, matching real AWS behavior.

| `storage.mode` | Volume on resource delete |
|---|---|
| `memory` | **Always removed** — memory mode implies no persistence across restarts |
| `persistent` / `hybrid` / `wal` | Retained (default) — remove with `FLOCI_STORAGE_PRUNE_VOLUMES_ON_DELETE=true` |

```bash
# Remove named volumes immediately when a resource is deleted (useful in CI with persistent mode)
FLOCI_STORAGE_PRUNE_VOLUMES_ON_DELETE=true
```

### Host-path mode (advanced)

Set `FLOCI_STORAGE_HOST_PERSISTENT_PATH` to an **absolute host path** to use bind mounts instead
of named volumes. This is only needed when you must access the container data directly from the
host filesystem.

```bash
FLOCI_STORAGE_HOST_PERSISTENT_PATH=/absolute/host/path/data
```

!!! warning
    `FLOCI_STORAGE_HOST_PERSISTENT_PATH` must be an absolute path (starting with `/`). Volume
    names and relative paths are not supported and will be silently ignored, falling back to
    named-volume mode.

## Environment Variable Override

```bash
FLOCI_STORAGE_MODE=persistent
FLOCI_STORAGE_PERSISTENT_PATH=/app/data
```

## Recommended Profiles

=== "Fast CI"

    All in memory, fastest possible startup and test execution:

    ```yaml
    floci:
      storage:
        mode: memory
    ```

=== "Local development"

    Hybrid — survive restarts without slowing down writes:

    ```yaml
    floci:
      storage:
        mode: hybrid
        persistent-path: ./data
    ```

=== "Durable development"

    Persistent — every write is immediately on disk:

    ```yaml
    floci:
      storage:
        mode: persistent
        persistent-path: ./data
    ```