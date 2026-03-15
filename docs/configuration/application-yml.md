# application.yml Reference

All settings can be provided as YAML (mounted as a config file or in `src/main/resources/application.yml`) or overridden via environment variables (`FLOCI_` prefix, dots/dashes replaced with underscores).

## Full Reference

```yaml
floci:
  base-url: "http://localhost:4566"  # Used to build callback URLs (e.g. SNS subscription endpoints)
  default-region: us-east-1
  default-account-id: "000000000000"

  auth:
    validate-signatures: false        # Set to true to enforce AWS request signing
    presign-secret: local-emulator-secret  # HMAC secret for S3 pre-signed URL verification

  storage:
    mode: memory                      # memory | persistent | hybrid | wal
    persistent-path: ./data
    wal:
      compaction-interval-ms: 30000
    services:
      ssm:
        mode: memory
        flush-interval-ms: 5000
      sqs:
        mode: memory
      s3:
        mode: memory
      dynamodb:
        mode: memory
        flush-interval-ms: 5000
      sns:
        mode: memory
        flush-interval-ms: 5000
      lambda:
        mode: memory
        flush-interval-ms: 5000
      cloudwatchlogs:
        mode: memory
        flush-interval-ms: 5000
      cloudwatchmetrics:
        mode: memory
        flush-interval-ms: 5000
      secretsmanager:
        mode: memory
        flush-interval-ms: 5000

  services:
    ssm:
      enabled: true
      max-parameter-history: 5        # Max versions kept per parameter

    sqs:
      enabled: true
      default-visibility-timeout: 30  # Seconds
      max-message-size: 262144        # Bytes (256 KB)

    s3:
      enabled: true
      default-presign-expiry-seconds: 3600

    dynamodb:
      enabled: true

    sns:
      enabled: true

    lambda:
      enabled: true
      ephemeral: false                # true = remove container after each invocation
      default-memory-mb: 128
      default-timeout-seconds: 3
      docker-host: unix:///var/run/docker.sock
      runtime-api-base-port: 9200    # Port range for Lambda Runtime API
      runtime-api-max-port: 9299
      code-path: ./data/lambda-code  # Where ZIP archives are stored
      poll-interval-ms: 1000
      container-idle-timeout-seconds: 300  # Remove idle containers after this

    apigateway:
      enabled: true

    iam:
      enabled: true

    elasticache:
      enabled: true
      proxy-base-port: 6379
      proxy-max-port: 6399
      default-image: "valkey/valkey:8"

    rds:
      enabled: true
      proxy-base-port: 7001
      proxy-max-port: 7099
      default-postgres-image: "postgres:16-alpine"
      default-mysql-image: "mysql:8.0"
      default-mariadb-image: "mariadb:11"

    eventbridge:
      enabled: true

    cloudwatchlogs:
      enabled: true
      max-events-per-query: 10000

    cloudwatchmetrics:
      enabled: true

    secretsmanager:
      enabled: true
      default-recovery-window-days: 30

    kinesis:
      enabled: true

    kms:
      enabled: true

    cognito:
      enabled: true

    stepfunctions:
      enabled: true

    cloudformation:
      enabled: true
```

## Disabling Services

Set `enabled: false` for any service you don't need. Disabled services return a `ServiceUnavailableException` rather than silently ignoring calls.

```yaml
floci:
  services:
    cloudformation:
      enabled: false
    stepfunctions:
      enabled: false
```