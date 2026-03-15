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
    mode: hybrid              # default for all services
    persistent-path: ./data   # base directory for all persistent data
    wal:
      compaction-interval-ms: 30000
```

## Per-Service Override

```yaml title="application.yml"
floci:
  storage:
    mode: memory
    services:
      dynamodb:
        mode: persistent
        flush-interval-ms: 5000
      s3:
        mode: hybrid
      sqs:
        mode: memory
```

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