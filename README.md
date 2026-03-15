<p align="center">
  <img src="logo.svg" alt="Floci" width="400" />
</p>

<h3 align="center">🍿☁️ Light, fluffy, and always free</h3>

<p align="center">
  <em>Named after <a href="https://en.wikipedia.org/wiki/Cirrocumulus_floccus">floccus</a> — the cloud formation that looks exactly like popcorn.</em>
</p>

A fast, free, and open-source local AWS service emulator built for developers who need reliable AWS services in
development without the cost, complexity, or vendor lock-in.

Floci runs as a single binary on port 4566 and emulates SSM Parameter Store, SQS, SNS, S3, DynamoDB, DynamoDB Streams,
Lambda, API Gateway (REST and HTTP), Cognito, KMS, Kinesis, Secrets Manager, CloudFormation, Step Functions, IAM, STS,
ElastiCache, RDS, EventBridge, and CloudWatch with full AWS CLI and SDK compatibility.

## Why Floci?

**No account required.** No auth tokens, no sign-ups, no telemetry. Pull the image and start building.

**No feature gates.** Every feature is available to everyone. SSM, SQS, SNS, S3, DynamoDB, DynamoDB Streams, Lambda, API
Gateway (REST and HTTP), Cognito, KMS, Kinesis, Secrets Manager, CloudFormation, Step Functions, IAM, STS, ElastiCache (
Redis with IAM auth), RDS (PostgreSQL and MySQL with IAM auth), EventBridge, CloudWatch Logs and Metrics — all included,
all free, forever.

**No CI restrictions.** Run Floci in your CI pipeline with zero limitations. No credits, no quotas, no paid tiers for
automated testing.

**Truly open source.** MIT licensed. Fork it, extend it, embed it. No "community edition" sunset coming.

> With LocalStack's community
> edition [sunsetting in March 2026](https://blog.localstack.cloud/the-road-ahead-for-localstack/) -- requiring auth
> tokens, dropping CI support from the free tier, and ceasing security updates -- Floci offers a stable,
> no-strings-attached alternative for teams that need local AWS services without commercial dependencies.

## Quick Start

### Docker (Recommended)

`latest` is the native image — sub-second startup, minimal memory:

```yaml
# docker-compose.yml
services:
  floci:
    image: hectorvent/floci:latest
    ports:
      - "4566:4566"
    volumes:
      - ./data:/app/data
```

```bash
docker compose up
```

All services are available at `http://localhost:4566`.

### JVM Image

For broader platform compatibility, use the JVM image:

```yaml
services:
  floci:
    image: hectorvent/floci:latest-jvm
    ports:
      - "4566:4566"
    volumes:
      - ./data:/app/data
```

### Image Tags

| Tag | Description |
|---|---|
| `latest` | Native image — sub-second startup (**default**) |
| `x.y.z` | Native image — pinned release |
| `latest-jvm` | JVM image — most compatible |
| `x.y.z-jvm` | JVM image — pinned release |

### Build from Source

```bash
git clone https://github.com/hectorvent/floci.git
cd floci
mvn quarkus:dev    # Dev mode with hot reload on port 4566
```

## Usage

Point any AWS CLI or SDK to Floci using `--endpoint-url`:

```bash
export AWS_ENDPOINT=http://localhost:4566
export AWS_DEFAULT_REGION=us-east-1
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
```

### SSM Parameter Store

```bash
aws ssm put-parameter --endpoint-url $AWS_ENDPOINT \
  --name /app/db/host --value "localhost" --type String

aws ssm get-parameter --endpoint-url $AWS_ENDPOINT \
  --name /app/db/host

aws ssm get-parameters-by-path --endpoint-url $AWS_ENDPOINT \
  --path /app/ --recursive
```

### SQS

```bash
aws sqs create-queue --endpoint-url $AWS_ENDPOINT \
  --queue-name my-queue

aws sqs send-message --endpoint-url $AWS_ENDPOINT \
  --queue-url $AWS_ENDPOINT/000000000000/my-queue \
  --message-body '{"event":"order.created"}'

aws sqs receive-message --endpoint-url $AWS_ENDPOINT \
  --queue-url $AWS_ENDPOINT/000000000000/my-queue
```

### S3

```bash
aws s3 mb s3://my-bucket --endpoint-url $AWS_ENDPOINT
aws s3 cp myfile.txt s3://my-bucket/ --endpoint-url $AWS_ENDPOINT
aws s3 ls s3://my-bucket --endpoint-url $AWS_ENDPOINT
```

### DynamoDB

```bash
aws dynamodb create-table --endpoint-url $AWS_ENDPOINT \
  --table-name Users \
  --attribute-definitions AttributeName=userId,AttributeType=S \
  --key-schema AttributeName=userId,KeyType=HASH \
  --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

aws dynamodb put-item --endpoint-url $AWS_ENDPOINT \
  --table-name Users \
  --item '{"userId":{"S":"u1"},"name":{"S":"Alice"}}'

aws dynamodb get-item --endpoint-url $AWS_ENDPOINT \
  --table-name Users --key '{"userId":{"S":"u1"}}'
```

### IAM

```bash
aws iam create-user --endpoint-url $AWS_ENDPOINT --user-name alice
aws iam create-role --endpoint-url $AWS_ENDPOINT \
  --role-name my-role \
  --assume-role-policy-document '{"Version":"2012-10-17","Statement":[]}'
aws iam attach-role-policy --endpoint-url $AWS_ENDPOINT \
  --role-name my-role --policy-arn arn:aws:iam::aws:policy/ReadOnlyAccess
```

### STS

```bash
aws sts get-caller-identity --endpoint-url $AWS_ENDPOINT

aws sts assume-role --endpoint-url $AWS_ENDPOINT \
  --role-arn arn:aws:iam::000000000000:role/my-role \
  --role-session-name my-session
```

## SDK Integration

```java
// Java (AWS SDK v2)
DynamoDbClient client = DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:4566"))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .build();
```

```python
# Python (boto3)
import boto3
client = boto3.client('ssm',
    endpoint_url='http://localhost:4566',
    region_name='us-east-1',
    aws_access_key_id='test',
    aws_secret_access_key='test')
```

```javascript
// Node.js (AWS SDK v3)
import {SSMClient} from "@aws-sdk/client-ssm";

const client = new SSMClient({
    endpoint: "http://localhost:4566",
    region: "us-east-1",
    credentials: {accessKeyId: "test", secretAccessKey: "test"},
});
```

## Supported Operations

### SSM Parameter Store (12 operations)

| Operation                | Description                           |
|--------------------------|---------------------------------------|
| `PutParameter`           | Create or update a parameter          |
| `GetParameter`           | Retrieve a single parameter           |
| `GetParameters`          | Retrieve multiple parameters by name  |
| `GetParametersByPath`    | Retrieve parameters by path hierarchy |
| `GetParameterHistory`    | Get version history of a parameter    |
| `LabelParameterVersion`  | Attach labels to a parameter version  |
| `DescribeParameters`     | List parameters with optional filters |
| `DeleteParameter`        | Delete a single parameter             |
| `DeleteParameters`       | Delete multiple parameters            |
| `AddTagsToResource`      | Tag a parameter                       |
| `ListTagsForResource`    | List parameter tags                   |
| `RemoveTagsFromResource` | Remove parameter tags                 |

### SQS (17 operations)

| Operation                      | Description                              |
|--------------------------------|------------------------------------------|
| `CreateQueue`                  | Create a standard or FIFO queue          |
| `DeleteQueue`                  | Delete a queue                           |
| `ListQueues`                   | List all queues                          |
| `GetQueueUrl`                  | Get queue URL by name                    |
| `GetQueueAttributes`           | Get queue attributes                     |
| `SetQueueAttributes`           | Set queue attributes                     |
| `SendMessage`                  | Send a message (FIFO supported)          |
| `SendMessageBatch`             | Send up to 10 messages in one request    |
| `ReceiveMessage`               | Receive messages with visibility timeout |
| `DeleteMessage`                | Delete a message by receipt handle       |
| `DeleteMessageBatch`           | Delete up to 10 messages in one request  |
| `ChangeMessageVisibility`      | Change message visibility timeout        |
| `ChangeMessageVisibilityBatch` | Change visibility for multiple messages  |
| `PurgeQueue`                   | Delete all messages in a queue           |
| `TagQueue`                     | Tag a queue                              |
| `UntagQueue`                   | Remove queue tags                        |
| `ListQueueTags`                | List queue tags                          |

### SNS (13 operations)

| Operation                  | Description                      |
|----------------------------|----------------------------------|
| `CreateTopic`              | Create a topic                   |
| `DeleteTopic`              | Delete a topic                   |
| `GetTopicAttributes`       | Get topic attributes             |
| `SetTopicAttributes`       | Set topic attributes             |
| `ListTopics`               | List all topics                  |
| `Subscribe`                | Subscribe an endpoint to a topic |
| `Unsubscribe`              | Remove a subscription            |
| `ListSubscriptions`        | List all subscriptions           |
| `ListSubscriptionsByTopic` | List subscriptions for a topic   |
| `Publish`                  | Publish a message to a topic     |
| `TagResource`              | Tag a topic                      |
| `UntagResource`            | Remove topic tags                |
| `ListTagsForResource`      | List topic tags                  |

### S3 (30 operations)

| Operation                            | Description                                         |
|--------------------------------------|-----------------------------------------------------|
| `CreateBucket`                       | Create a bucket (with optional Object Lock)         |
| `DeleteBucket`                       | Delete a bucket                                     |
| `ListBuckets`                        | List all buckets                                    |
| `HeadBucket`                         | Check bucket existence                              |
| `GetBucketLocation`                  | Get bucket region                                   |
| `PutObject`                          | Upload an object                                    |
| `GetObject`                          | Download an object                                  |
| `HeadObject`                         | Get object metadata                                 |
| `DeleteObject`                       | Delete an object                                    |
| `DeleteObjects`                      | Delete multiple objects in one request              |
| `CopyObject`                         | Copy an object within or between buckets            |
| `ListObjects` / `ListObjectsV2`      | List objects with prefix/delimiter support          |
| `PutBucketTagging`                   | Tag a bucket                                        |
| `GetBucketTagging`                   | Get bucket tags                                     |
| `DeleteBucketTagging`                | Remove bucket tags                                  |
| `PutObjectTagging`                   | Tag an object                                       |
| `GetObjectTagging`                   | Get object tags                                     |
| `DeleteObjectTagging`                | Remove object tags                                  |
| `PutBucketVersioning`                | Enable or suspend versioning                        |
| `GetBucketVersioning`                | Get versioning status                               |
| `ListObjectVersions`                 | List object versions                                |
| `InitiateMultipartUpload`            | Start a multipart upload                            |
| `UploadPart`                         | Upload a part                                       |
| `CompleteMultipartUpload`            | Complete a multipart upload                         |
| `AbortMultipartUpload`               | Abort a multipart upload                            |
| `ListMultipartUploads`               | List active multipart uploads                       |
| `PutBucketNotificationConfiguration` | Configure S3 event notifications (SQS/SNS)          |
| `GetBucketNotificationConfiguration` | Get notification configuration                      |
| `PutObjectLockConfiguration`         | Set default retention policy on a bucket            |
| `GetObjectLockConfiguration`         | Get bucket-level Object Lock config                 |
| `PutObjectRetention`                 | Set COMPLIANCE or GOVERNANCE retention on an object |
| `GetObjectRetention`                 | Get per-object retention settings                   |
| `PutObjectLegalHold`                 | Apply or lift a legal hold on an object             |
| `GetObjectLegalHold`                 | Get legal hold status                               |
| Pre-signed URLs                      | Generate time-limited GET/PUT URLs (AWS Sig V4)     |

### DynamoDB (22 operations)

| Operation            | Description                                               |
|----------------------|-----------------------------------------------------------|
| `CreateTable`        | Create a table with key schema, GSIs, and LSIs            |
| `DeleteTable`        | Delete a table                                            |
| `DescribeTable`      | Get table metadata                                        |
| `ListTables`         | List all tables                                           |
| `UpdateTable`        | Modify throughput and table settings                      |
| `DescribeTimeToLive` | Get TTL configuration                                     |
| `UpdateTimeToLive`   | Enable or disable TTL on a table                          |
| `PutItem`            | Insert or replace an item                                 |
| `GetItem`            | Retrieve a single item by key                             |
| `DeleteItem`         | Delete an item by key                                     |
| `UpdateItem`         | Update item (UpdateExpression supported)                  |
| `Query`              | Query with key conditions and filters (GSI/LSI supported) |
| `Scan`               | Scan with filter expressions                              |
| `BatchWriteItem`     | Put or delete up to 25 items                              |
| `BatchGetItem`       | Get up to 100 items across tables                         |
| `TransactWriteItems` | Execute multiple write operations atomically              |
| `TransactGetItems`   | Get multiple items atomically                             |
| `TagResource`        | Tag a table                                               |
| `UntagResource`      | Remove table tags                                         |
| `ListTagsOfResource` | List table tags                                           |

### DynamoDB Streams (5 operations)

| Operation          | Description                                       |
|--------------------|---------------------------------------------------|
| `ListStreams`      | List streams for a table                          |
| `DescribeStream`   | Get stream metadata and shard info                |
| `GetShardIterator` | Get a shard iterator                              |
| `GetRecords`       | Read records from a stream shard                  |
| Stream triggers    | DynamoDB Streams as a Lambda event source mapping |

### Lambda (25 operations)

| Operation                     | Description                                                                |
|-------------------------------|----------------------------------------------------------------------------|
| `CreateFunction`              | Create a function (Node.js, Python, Java runtimes)                         |
| `GetFunction`                 | Get function metadata and code location                                    |
| `GetFunctionConfiguration`    | Get function configuration                                                 |
| `ListFunctions`               | List all functions                                                         |
| `UpdateFunctionCode`          | Update function code                                                       |
| `UpdateFunctionConfiguration` | Update environment, memory, timeout, and other settings                    |
| `DeleteFunction`              | Delete a function                                                          |
| `Invoke`                      | Invoke synchronously (RequestResponse), asynchronously (Event), or dry-run |
| `CreateAlias`                 | Create a function alias pointing to a version                              |
| `GetAlias`                    | Get alias details                                                          |
| `ListAliases`                 | List all aliases for a function                                            |
| `UpdateAlias`                 | Update alias target version                                                |
| `DeleteAlias`                 | Delete an alias                                                            |
| `CreateFunctionUrlConfig`     | Create an HTTP URL for a function                                          |
| `GetFunctionUrlConfig`        | Get function URL configuration                                             |
| `UpdateFunctionUrlConfig`     | Update function URL settings                                               |
| `DeleteFunctionUrlConfig`     | Delete a function URL                                                      |
| `ListFunctionUrlConfigs`      | List function URLs                                                         |
| `CreateEventSourceMapping`    | Map SQS, Kinesis, or DynamoDB Streams to trigger a function                |
| `GetEventSourceMapping`       | Get mapping details                                                        |
| `ListEventSourceMappings`     | List all mappings                                                          |
| `UpdateEventSourceMapping`    | Change batch size or state                                                 |
| `DeleteEventSourceMapping`    | Remove a mapping                                                           |

### API Gateway (24 operations)

| Operation                                   | Description                                        |
|---------------------------------------------|----------------------------------------------------|
| `CreateRestApi`                             | Create a REST API                                  |
| `GetRestApi` / `GetRestApis`                | Get one or all REST APIs                           |
| `DeleteRestApi`                             | Delete a REST API                                  |
| `CreateResource`                            | Add a resource path                                |
| `GetResource` / `GetResources`              | Get one or all resources                           |
| `DeleteResource`                            | Delete a resource                                  |
| `PutMethod`                                 | Add an HTTP method to a resource                   |
| `GetMethod`                                 | Get method configuration                           |
| `PutMethodResponse`                         | Configure method response                          |
| `PutIntegration`                            | Set backend integration (MOCK or AWS_PROXY/Lambda) |
| `GetIntegration`                            | Get integration details                            |
| `PutIntegrationResponse`                    | Configure integration response                     |
| `GetIntegrationResponse`                    | Get integration response                           |
| `CreateDeployment`                          | Deploy an API snapshot                             |
| `CreateStage`                               | Create a named stage                               |
| `GetStage` / `GetStages`                    | Get one or all stages                              |
| `UpdateStage`                               | Update stage variables or settings                 |
| `DeleteStage`                               | Delete a stage                                     |
| `TagResource` / `UntagResource` / `GetTags` | Tag management                                     |
| Stage invocation                            | Execute APIs via `/{stage}/{path}` routing         |

### IAM (65+ operations)

| Category           | Operations                                                                                                                                                                                                                       |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Users              | `CreateUser`, `GetUser`, `ListUsers`, `UpdateUser`, `DeleteUser`, `TagUser`, `UntagUser`, `ListUserTags`                                                                                                                         |
| Access Keys        | `CreateAccessKey`, `ListAccessKeys`, `UpdateAccessKey`, `DeleteAccessKey`                                                                                                                                                        |
| Groups             | `CreateGroup`, `GetGroup`, `ListGroups`, `DeleteGroup`, `AddUserToGroup`, `RemoveUserFromGroup`, `ListGroupsForUser`                                                                                                             |
| Roles              | `CreateRole`, `GetRole`, `ListRoles`, `UpdateRole`, `DeleteRole`, `UpdateAssumeRolePolicy`, `TagRole`, `UntagRole`, `ListRoleTags`                                                                                               |
| Policies           | `CreatePolicy`, `GetPolicy`, `ListPolicies`, `DeletePolicy`, `CreatePolicyVersion`, `GetPolicyVersion`, `ListPolicyVersions`, `DeletePolicyVersion`, `SetDefaultPolicyVersion`, `TagPolicy`, `UntagPolicy`, `ListPolicyTags`     |
| Policy Attachments | `AttachRolePolicy`, `DetachRolePolicy`, `ListAttachedRolePolicies`, `AttachUserPolicy`, `DetachUserPolicy`, `ListAttachedUserPolicies`, `AttachGroupPolicy`, `DetachGroupPolicy`, `ListAttachedGroupPolicies`                    |
| Inline Policies    | `PutRolePolicy`, `GetRolePolicy`, `ListRolePolicies`, `DeleteRolePolicy`, `PutUserPolicy`, `GetUserPolicy`, `ListUserPolicies`, `DeleteUserPolicy`, `PutGroupPolicy`, `GetGroupPolicy`, `ListGroupPolicies`, `DeleteGroupPolicy` |
| Instance Profiles  | `CreateInstanceProfile`, `GetInstanceProfile`, `ListInstanceProfiles`, `DeleteInstanceProfile`, `AddRoleToInstanceProfile`, `RemoveRoleFromInstanceProfile`, `ListInstanceProfilesForRole`                                       |

### STS (7 operations)

| Operation                    | Description                                           |
|------------------------------|-------------------------------------------------------|
| `GetCallerIdentity`          | Return account ID, user ID, and ARN                   |
| `AssumeRole`                 | Return temporary credentials for a role               |
| `AssumeRoleWithWebIdentity`  | Assume a role via OIDC/web identity token             |
| `AssumeRoleWithSAML`         | Assume a role via SAML assertion                      |
| `GetSessionToken`            | Return temporary credentials for the current identity |
| `GetFederationToken`         | Return credentials for a federated user               |
| `DecodeAuthorizationMessage` | Decode an encoded authorization failure message       |

### ElastiCache (9 operations + Redis protocol)

| Operation                   | Description                                                                                 |
|-----------------------------|---------------------------------------------------------------------------------------------|
| `CreateReplicationGroup`    | Create a Redis replication group                                                            |
| `DescribeReplicationGroups` | List replication groups                                                                     |
| `DeleteReplicationGroup`    | Delete a replication group                                                                  |
| `CreateUser`                | Create a Redis user with password or IAM auth                                               |
| `DescribeUsers`             | List Redis users                                                                            |
| `ModifyUser`                | Update user passwords or access string                                                      |
| `DeleteUser`                | Delete a Redis user                                                                         |
| `GenerateIamAuthToken`      | Generate a Sig V4 token for Redis IAM auth                                                  |
| `ValidateIamAuthToken`      | Validate a previously generated token                                                       |
| Redis protocol              | Full Redis command support via TCP (SET, GET, PING, AUTH, etc.) using real Redis containers |

### RDS (14 operations + JDBC connectivity)

| Operation                   | Description                                                                                   |
|-----------------------------|-----------------------------------------------------------------------------------------------|
| `CreateDBInstance`          | Create a PostgreSQL or MySQL DB instance                                                      |
| `DescribeDBInstances`       | List DB instances                                                                             |
| `ModifyDBInstance`          | Modify instance settings (e.g., enable IAM auth)                                              |
| `RebootDBInstance`          | Reboot an instance                                                                            |
| `DeleteDBInstance`          | Delete an instance                                                                            |
| `CreateDBCluster`           | Create a MySQL/Aurora-compatible cluster                                                      |
| `DescribeDBClusters`        | List DB clusters                                                                              |
| `ModifyDBCluster`           | Modify cluster settings                                                                       |
| `DeleteDBCluster`           | Delete a cluster                                                                              |
| `CreateDBParameterGroup`    | Create a parameter group                                                                      |
| `DescribeDBParameterGroups` | List parameter groups                                                                         |
| `DescribeDBParameters`      | List parameters in a group                                                                    |
| `ModifyDBParameterGroup`    | Update parameter values                                                                       |
| `DeleteDBParameterGroup`    | Delete a parameter group                                                                      |
| IAM Auth                    | Generate and validate RDS IAM authentication tokens                                           |
| JDBC                        | Real PostgreSQL and MySQL containers launched on demand, accessible via standard JDBC drivers |

### EventBridge (14 operations)

| Operation                    | Description                                 |
|------------------------------|---------------------------------------------|
| `CreateEventBus`             | Create a custom event bus                   |
| `DescribeEventBus`           | Get event bus details                       |
| `ListEventBuses`             | List all event buses                        |
| `DeleteEventBus`             | Delete an event bus                         |
| `PutRule`                    | Create or update an event rule              |
| `DescribeRule`               | Get rule details                            |
| `ListRules`                  | List rules on an event bus                  |
| `EnableRule` / `DisableRule` | Control whether a rule is active            |
| `DeleteRule`                 | Delete a rule                               |
| `PutTargets`                 | Attach targets (SQS, SNS, Lambda) to a rule |
| `ListTargetsByRule`          | List rule targets                           |
| `RemoveTargets`              | Detach targets from a rule                  |
| `PutEvents`                  | Publish custom events to an event bus       |

### CloudWatch Logs (14 operations)

| Operation                                            | Description                             |
|------------------------------------------------------|-----------------------------------------|
| `CreateLogGroup`                                     | Create a log group                      |
| `DeleteLogGroup`                                     | Delete a log group                      |
| `DescribeLogGroups`                                  | List log groups                         |
| `PutRetentionPolicy`                                 | Set log retention (days)                |
| `DeleteRetentionPolicy`                              | Remove a retention policy               |
| `TagLogGroup` / `UntagLogGroup` / `ListTagsLogGroup` | Tag management                          |
| `CreateLogStream`                                    | Create a log stream within a group      |
| `DeleteLogStream`                                    | Delete a log stream                     |
| `DescribeLogStreams`                                 | List streams in a group                 |
| `PutLogEvents`                                       | Ingest log events                       |
| `GetLogEvents`                                       | Retrieve log events with time filtering |
| `FilterLogEvents`                                    | Search log events by pattern            |

### CloudWatch Metrics (5 operations)

| Operation             | Description                                         |
|-----------------------|-----------------------------------------------------|
| `PutMetricData`       | Publish custom metrics with optional dimensions     |
| `ListMetrics`         | List metrics by namespace, name, or dimension       |
| `GetMetricStatistics` | Retrieve statistics (Sum, Average, Count, Min, Max) |
| `GetMetricData`       | Query metrics with metric math expressions          |
| `PutMetricAlarm`      | Create or update a metric alarm                     |

### API Gateway v2 / HTTP API (16 operations)

| Operation                            | Description                       |
|--------------------------------------|-----------------------------------|
| `CreateApi`                          | Create an HTTP API                |
| `GetApi` / `GetApis`                 | Get one or all HTTP APIs          |
| `DeleteApi`                          | Delete an HTTP API                |
| `CreateRoute`                        | Add a route to an API             |
| `GetRoute` / `GetRoutes`             | Get one or all routes             |
| `DeleteRoute`                        | Delete a route                    |
| `CreateIntegration`                  | Set a backend integration         |
| `GetIntegration` / `GetIntegrations` | Get one or all integrations       |
| `DeleteIntegration`                  | Delete an integration             |
| `CreateAuthorizer`                   | Create a JWT or Lambda authorizer |
| `GetAuthorizer` / `GetAuthorizers`   | Get one or all authorizers        |
| `DeleteAuthorizer`                   | Delete an authorizer              |
| `CreateStage`                        | Create a named deployment stage   |
| `GetStage` / `GetStages`             | Get one or all stages             |
| `UpdateStage`                        | Update stage settings             |
| `DeleteStage`                        | Delete a stage                    |

### Cognito (20 operations)

| Operation                   | Description                                                |
|-----------------------------|------------------------------------------------------------|
| `CreateUserPool`            | Create a user pool                                         |
| `DescribeUserPool`          | Get user pool details                                      |
| `ListUserPools`             | List all user pools                                        |
| `DeleteUserPool`            | Delete a user pool                                         |
| `CreateUserPoolClient`      | Create an app client                                       |
| `DescribeUserPoolClient`    | Get app client details                                     |
| `ListUserPoolClients`       | List app clients for a pool                                |
| `DeleteUserPoolClient`      | Delete an app client                                       |
| `AdminCreateUser`           | Create a user in a pool                                    |
| `AdminGetUser`              | Get a user by username                                     |
| `AdminDeleteUser`           | Delete a user                                              |
| `AdminSetUserPassword`      | Set a user's password                                      |
| `AdminUpdateUserAttributes` | Update user attributes                                     |
| `ListUsers`                 | List users in a pool                                       |
| `SignUp`                    | Register a new user                                        |
| `InitiateAuth`              | Start an authentication flow                               |
| `AdminInitiateAuth`         | Admin-initiated authentication                             |
| `RespondToAuthChallenge`    | Respond to an auth challenge                               |
| Well-known endpoints        | JWKS and OpenID configuration endpoints for JWT validation |

### KMS (15 operations)

| Operation                         | Description                                             |
|-----------------------------------|---------------------------------------------------------|
| `CreateKey`                       | Create a KMS key                                        |
| `DescribeKey`                     | Get key metadata                                        |
| `ListKeys`                        | List all keys                                           |
| `ScheduleKeyDeletion`             | Schedule a key for deletion                             |
| `CancelKeyDeletion`               | Cancel a pending deletion                               |
| `Encrypt`                         | Encrypt plaintext data                                  |
| `Decrypt`                         | Decrypt ciphertext                                      |
| `ReEncrypt`                       | Re-encrypt data under a different key                   |
| `GenerateDataKey`                 | Generate a data encryption key (plaintext + ciphertext) |
| `GenerateDataKeyWithoutPlaintext` | Generate a data key (ciphertext only)                   |
| `Sign`                            | Create a digital signature                              |
| `Verify`                          | Verify a digital signature                              |
| `CreateAlias`                     | Create a friendly alias for a key                       |
| `DeleteAlias`                     | Delete an alias                                         |
| `ListAliases`                     | List all aliases                                        |

### Kinesis (15 operations)

| Operation                                                        | Description                                |
|------------------------------------------------------------------|--------------------------------------------|
| `CreateStream`                                                   | Create a data stream                       |
| `DeleteStream`                                                   | Delete a stream                            |
| `ListStreams`                                                    | List all streams                           |
| `DescribeStream`                                                 | Get stream details and shard info          |
| `DescribeStreamSummary`                                          | Get a summary of stream metadata           |
| `PutRecord`                                                      | Write a single record                      |
| `PutRecords`                                                     | Write up to 500 records in one request     |
| `GetShardIterator`                                               | Get a position marker for a shard          |
| `GetRecords`                                                     | Read records from a shard                  |
| `SplitShard`                                                     | Split a shard into two                     |
| `MergeShards`                                                    | Merge two adjacent shards                  |
| `RegisterStreamConsumer`                                         | Register an enhanced fan-out consumer      |
| `DeregisterStreamConsumer`                                       | Remove a consumer                          |
| `SubscribeToShard`                                               | Subscribe to a shard with enhanced fan-out |
| `AddTagsToStream` / `RemoveTagsFromStream` / `ListTagsForStream` | Tag management                             |

### Secrets Manager (10 operations)

| Operation                                                          | Description                                     |
|--------------------------------------------------------------------|-------------------------------------------------|
| `CreateSecret`                                                     | Create a new secret                             |
| `GetSecretValue`                                                   | Retrieve a secret value by name or ARN          |
| `PutSecretValue`                                                   | Store a new version of a secret                 |
| `UpdateSecret`                                                     | Update secret metadata                          |
| `DescribeSecret`                                                   | Get secret details and version info             |
| `ListSecrets`                                                      | List all secrets                                |
| `DeleteSecret`                                                     | Delete a secret (with optional recovery window) |
| `ListSecretVersionIds`                                             | List versions of a secret                       |
| `TagResource` / `UntagResource`                                    | Tag management                                  |
| `GetResourcePolicy` / `PutResourcePolicy` / `DeleteResourcePolicy` | Resource-based policies                         |

### CloudFormation (12 operations)

| Operation               | Description                              |
|-------------------------|------------------------------------------|
| `CreateStack`           | Create a stack from a template           |
| `UpdateStack`           | Update an existing stack                 |
| `DeleteStack`           | Delete a stack and its resources         |
| `DescribeStacks`        | Get stack status and outputs             |
| `ListStacks`            | List stacks with optional status filter  |
| `DescribeStackEvents`   | Get the event history of a stack         |
| `ListStackResources`    | List resources in a stack                |
| `DescribeStackResource` | Get details of a specific stack resource |
| `CreateChangeSet`       | Preview changes before applying          |
| `DescribeChangeSet`     | Get change set details                   |
| `ExecuteChangeSet`      | Apply a change set to a stack            |
| `ListChangeSets`        | List change sets for a stack             |

### Step Functions (11 operations)

| Operation                                                   | Description                                   |
|-------------------------------------------------------------|-----------------------------------------------|
| `CreateStateMachine`                                        | Create a state machine from an ASL definition |
| `DescribeStateMachine`                                      | Get state machine metadata                    |
| `ListStateMachines`                                         | List all state machines                       |
| `UpdateStateMachine`                                        | Update the ASL definition or role             |
| `DeleteStateMachine`                                        | Delete a state machine                        |
| `StartExecution`                                            | Start a new execution                         |
| `DescribeExecution`                                         | Get execution status and output               |
| `ListExecutions`                                            | List executions for a state machine           |
| `StopExecution`                                             | Stop a running execution                      |
| `GetExecutionHistory`                                       | Get the event history of an execution         |
| `SendTaskSuccess` / `SendTaskFailure` / `SendTaskHeartbeat` | Respond to task tokens                        |

## Configuration

All settings can be overridden via environment variables. Floci uses the Quarkus convention: dots become underscores,
names become uppercase (e.g., `floci.default-region` becomes `FLOCI_DEFAULT_REGION`).

### General

| Variable                   | Default        | Description            |
|----------------------------|----------------|------------------------|
| `QUARKUS_HTTP_PORT`        | `4566`         | HTTP port              |
| `FLOCI_DEFAULT_REGION`     | `us-east-1`    | Default AWS region     |
| `FLOCI_DEFAULT_ACCOUNT_ID` | `000000000000` | Default AWS account ID |

### Storage

Floci supports 4 storage modes, configurable globally or per service:

| Mode         | Description                              | Best for                  |
|--------------|------------------------------------------|---------------------------|
| `memory`     | In-memory only, data lost on restart     | CI, throwaway tests       |
| `persistent` | Immediate disk writes on every change    | Data safety               |
| `hybrid`     | In-memory with async flush to disk       | Development (default)     |
| `wal`        | Write-ahead log with periodic compaction | High-throughput workloads |

| Variable                                   | Default  | Description                  |
|--------------------------------------------|----------|------------------------------|
| `FLOCI_STORAGE_MODE`                       | `hybrid` | Global storage mode          |
| `FLOCI_STORAGE_PERSISTENT_PATH`            | `./data` | Data directory               |
| `FLOCI_STORAGE_WAL_COMPACTION_INTERVAL_MS` | `30000`  | WAL compaction interval (ms) |

#### Per-Service Storage Overrides

Override the global mode for individual services:

| Variable                                                        | Default       | Description                          |
|-----------------------------------------------------------------|---------------|--------------------------------------|
| `FLOCI_STORAGE_SERVICES_SSM_MODE`                               | `memory`      | SSM storage mode                     |
| `FLOCI_STORAGE_SERVICES_SSM_FLUSH_INTERVAL_MS`                  | `5000`        | SSM flush interval (ms)              |
| `FLOCI_STORAGE_SERVICES_SQS_MODE`                               | `memory`      | SQS storage mode                     |
| `FLOCI_STORAGE_SERVICES_SQS_PERSIST_ON_SHUTDOWN`                | `true`        | Flush SQS messages to disk on shutdown |
| `FLOCI_STORAGE_SERVICES_S3_MODE`                                | `hybrid`      | S3 storage mode                      |
| `FLOCI_STORAGE_SERVICES_S3_CACHE_SIZE_MB`                       | `100`         | S3 in-memory cache size (MB)         |
| `FLOCI_STORAGE_SERVICES_DYNAMODB_MODE`                          | `memory`      | DynamoDB storage mode                |
| `FLOCI_STORAGE_SERVICES_DYNAMODB_FLUSH_INTERVAL_MS`             | `5000`        | DynamoDB flush interval (ms)         |
| `FLOCI_STORAGE_SERVICES_SNS_MODE`                               | `memory`      | SNS storage mode                     |
| `FLOCI_STORAGE_SERVICES_SNS_FLUSH_INTERVAL_MS`                  | `5000`        | SNS flush interval (ms)              |
| `FLOCI_STORAGE_SERVICES_LAMBDA_MODE`                            | `memory`      | Lambda storage mode                  |
| `FLOCI_STORAGE_SERVICES_LAMBDA_FLUSH_INTERVAL_MS`               | `5000`        | Lambda flush interval (ms)           |
| `FLOCI_STORAGE_SERVICES_APIGATEWAY_MODE`                        | `memory`      | API Gateway (v1) storage mode        |
| `FLOCI_STORAGE_SERVICES_APIGATEWAY_FLUSH_INTERVAL_MS`           | `5000`        | API Gateway (v1) flush interval (ms) |
| `FLOCI_STORAGE_SERVICES_APIGATEWAYV2_MODE`                      | `memory`      | API Gateway (v2) storage mode        |
| `FLOCI_STORAGE_SERVICES_APIGATEWAYV2_FLUSH_INTERVAL_MS`         | `5000`        | API Gateway (v2) flush interval (ms) |
| `FLOCI_STORAGE_SERVICES_IAM_MODE`                               | `memory`      | IAM storage mode                     |
| `FLOCI_STORAGE_SERVICES_IAM_FLUSH_INTERVAL_MS`                  | `5000`        | IAM flush interval (ms)              |
| `FLOCI_STORAGE_SERVICES_RDS_MODE`                               | `memory`      | RDS storage mode                     |
| `FLOCI_STORAGE_SERVICES_RDS_FLUSH_INTERVAL_MS`                  | `5000`        | RDS flush interval (ms)              |
| `FLOCI_STORAGE_SERVICES_EVENTBRIDGE_MODE`                       | `memory`      | EventBridge storage mode             |
| `FLOCI_STORAGE_SERVICES_EVENTBRIDGE_FLUSH_INTERVAL_MS`          | `5000`        | EventBridge flush interval (ms)      |
| `FLOCI_STORAGE_SERVICES_CLOUDWATCHLOGS_MODE`                    | `memory`      | CloudWatch Logs storage mode         |
| `FLOCI_STORAGE_SERVICES_CLOUDWATCHLOGS_FLUSH_INTERVAL_MS`       | `5000`        | CloudWatch Logs flush interval (ms)  |
| `FLOCI_STORAGE_SERVICES_CLOUDWATCHMETRICS_MODE`                 | `memory`      | CloudWatch Metrics storage mode      |
| `FLOCI_STORAGE_SERVICES_CLOUDWATCHMETRICS_FLUSH_INTERVAL_MS`    | `5000`        | CloudWatch Metrics flush interval (ms) |
| `FLOCI_STORAGE_SERVICES_SECRETSMANAGER_MODE`                    | `memory`      | Secrets Manager storage mode         |
| `FLOCI_STORAGE_SERVICES_SECRETSMANAGER_FLUSH_INTERVAL_MS`       | `5000`        | Secrets Manager flush interval (ms)  |

### Auth

| Variable                         | Default                 | Description                     |
|----------------------------------|-------------------------|---------------------------------|
| `FLOCI_AUTH_VALIDATE_SIGNATURES` | `false`                 | Validate AWS request signatures |
| `FLOCI_AUTH_REQUIRE_CREDENTIALS` | `false`                 | Require AWS credentials         |
| `FLOCI_AUTH_PRESIGN_SECRET`      | `local-emulator-secret` | Secret for S3 pre-signed URLs   |

### Service Limits

| Variable                                           | Default  | Description                  |
|----------------------------------------------------|----------|------------------------------|
| `FLOCI_SERVICES_SSM_MAX_PARAMETER_HISTORY`         | `5`      | Max parameter versions kept  |
| `FLOCI_SERVICES_SQS_DEFAULT_VISIBILITY_TIMEOUT`    | `30`     | Visibility timeout (seconds) |
| `FLOCI_SERVICES_SQS_MAX_MESSAGE_SIZE`              | `262144` | Max message size (bytes)     |
| `FLOCI_SERVICES_SQS_MAX_RECEIVE_COUNT`             | `10`     | Max receive count            |
| `FLOCI_SERVICES_S3_MAX_MULTIPART_PARTS`            | `10000`  | Max parts per upload         |
| `FLOCI_SERVICES_S3_DEFAULT_PRESIGN_EXPIRY_SECONDS` | `3600`   | Pre-signed URL expiry        |
| `FLOCI_SERVICES_DYNAMODB_MAX_ITEM_SIZE`            | `400000` | Max item size (bytes)        |
| `FLOCI_SERVICES_DOCKER_NETWORK`                    |          | Shared Docker network for Lambda, RDS, ElastiCache containers |

## Docker Compose for Developers

### Development (with persistence)

```yaml
services:
  floci:
    image: hectorvent/floci:latest
    ports:
      - "4566:4566"
    volumes:
      - ./data:/app/data
    environment:
      FLOCI_STORAGE_MODE: hybrid
      FLOCI_STORAGE_PERSISTENT_PATH: /app/data
```

### CI / Testing (ephemeral)

```yaml
services:
  floci:
    image: hectorvent/floci:latest
    ports:
      - "4566:4566"
    environment:
      FLOCI_STORAGE_MODE: memory
```

### Full-Stack Development

```yaml
services:
  floci:
    image: hectorvent/floci:latest
    ports:
      - "4566:4566"
    volumes:
      - floci-data:/app/data
    environment:
      FLOCI_STORAGE_MODE: hybrid
      FLOCI_STORAGE_PERSISTENT_PATH: /app/data

  api:
    build: ./api
    depends_on:
      - floci
    environment:
      AWS_ENDPOINT_URL: http://floci:4566
      AWS_DEFAULT_REGION: us-east-1
      AWS_ACCESS_KEY_ID: test
      AWS_SECRET_ACCESS_KEY: test

  worker:
    build: ./worker
    depends_on:
      - floci
    environment:
      AWS_ENDPOINT_URL: http://floci:4566
      AWS_DEFAULT_REGION: us-east-1
      AWS_ACCESS_KEY_ID: test
      AWS_SECRET_ACCESS_KEY: test

volumes:
  floci-data:
```

### Custom Region and Account

```yaml
services:
  floci:
    image: hectorvent/floci:latest
    ports:
      - "4566:4566"
    volumes:
      - ./data:/app/data
    environment:
      FLOCI_DEFAULT_REGION: eu-west-1
      FLOCI_DEFAULT_ACCOUNT_ID: "123456789012"
      FLOCI_STORAGE_MODE: hybrid
      FLOCI_STORAGE_PERSISTENT_PATH: /app/data
      # Use persistent storage for SSM, memory for SQS
      FLOCI_STORAGE_SERVICES_SSM_MODE: persistent
      FLOCI_STORAGE_SERVICES_SQS_MODE: memory
```

## Data Persistence

When using `persistent` or `hybrid` mode, data is stored as human-readable JSON files:

```
data/
  ssm-parameters.json      # SSM parameters
  ssm-history.json          # Parameter version history
  sqs-queues.json           # Queue definitions
  sqs-messages.json         # Queue messages
  s3-buckets.json           # Bucket metadata
  s3-objects.json            # Object metadata
  s3/                        # Object binary data
  dynamodb-tables.json      # Table definitions
  dynamodb-items.json       # Table items
```

You can inspect, edit, or seed these files directly -- they're plain JSON.

## Region Isolation

SSM, SQS, SNS, DynamoDB, DynamoDB Streams, Lambda, API Gateway, Cognito, KMS, Kinesis, Secrets Manager, CloudFormation,
Step Functions, EventBridge, and CloudWatch resources are isolated by region. Different regions maintain separate
namespaces, matching AWS behavior. S3 buckets and IAM are global.

## Building

```bash
mvn clean package                  # Build JAR
mvn clean package -DskipTests      # Build without tests
mvn test                           # Run all tests

# Docker images
docker build -t floci:latest-jvm .                   # JVM image
docker build -f Dockerfile.native -t floci:native .  # Native image (~152MB)
```

## Performance

Benchmarks run against the AWS SDK v2 test suite on Apple Silicon M-series host.

### JVM vs Native — Startup & Memory

|                   | JVM      | Native     | Delta      |
|-------------------|----------|------------|------------|
| Startup time      | 684 ms   | **24 ms**  | 28× faster |
| Idle memory       | 78 MiB   | **13 MiB** | −83%       |
| Memory under load | 176 MiB  | **80 MiB** | −55%       |

### JVM vs Native — Lambda Invocation Latency (110 sequential calls)

|              | JVM      | Native     | Delta       |
|--------------|----------|------------|-------------|
| Cold start   | 1000 ms  | **158 ms** | 6.3× faster |
| Warm average | 3 ms     | **2 ms**   | 1.5× faster |
| Warm minimum | 2 ms     | **1 ms**   |             |
| Warm maximum | 15 ms    | **6 ms**   |             |

### JVM vs Native — Throughput (10,000 invocations / 10 threads)

|             | JVM       | Native        | Delta  |
|-------------|-----------|---------------|--------|
| Throughput  | 280 req/s | **289 req/s** | +3.2%  |
| Latency avg | 13 ms     | **12 ms**     |        |
| Failures    | 0         | **0**         |        |

Both runtimes pass all 408 SDK tests. Throughput is roughly equivalent because both are I/O bound on Lambda Docker containers — the native advantage shows most clearly in startup time and memory footprint.

## Floci vs LocalStack Community

Benchmarks run against both emulators using the same AWS SDK v2 test suite (408 checks covering SQS, SQS→Lambda ESM, SNS, S3, S3 Object Lock, S3 Advanced, SSM, DynamoDB, DynamoDB Advanced, DynamoDB LSI, DynamoDB Streams, Lambda, API Gateway REST, API Gateway v2, S3 Event Notifications, IAM, STS, IAM Performance, EventBridge, CloudWatch Logs, CloudWatch Metrics, Secrets Manager, KMS, Cognito, Step Functions, Kinesis, ElastiCache, and RDS). LocalStack 4.14.0 community, Floci native 1.0.0.

### Test Results

| Test Suite                            | Floci Native  | LocalStack 4.14.0                                                                                           |
|---------------------------------------|---------------|-------------------------------------------------------------------------------------------------------------|
| SQS (22 checks)                       | ✅ 22/22       | ✅ 22/22                                                                                                     |
| SQS → Lambda ESM (12 checks)          | ✅ 12/12       | ⚠️ 10/12 — `CreateEventSourceMapping` response mismatch; non-existent function returns wrong error code     |
| SNS (12 checks)                       | ✅ 12/12       | ✅ 12/12                                                                                                     |
| S3 (23 checks)                        | ✅ 23/23       | ⚠️ 22/23 — `DeleteBucketTagging` returns error when tag set is empty                                       |
| S3 Object Lock (30 checks)            | ✅ 30/30       | ⚠️ 24/30 — COMPLIANCE/GOVERNANCE enforcement and legal holds not blocking deletes                          |
| S3 Advanced (13 checks)               | ✅ 13/13       | ⚠️ 10/13 — ACL put/get and RestoreObject not supported                                                     |
| SSM (12 checks)                       | ✅ 12/12       | ✅ 12/12                                                                                                     |
| DynamoDB (18 checks)                  | ✅ 18/18       | ✅ 18/18                                                                                                     |
| DynamoDB Advanced (18 checks)         | ✅ 18/18       | ⚠️ 16/18 — TTL expiry not enforced (items remain visible after TTL passes)                                 |
| DynamoDB LSI (4 checks)               | ✅ 4/4         | ✅ 4/4                                                                                                       |
| DynamoDB Streams (12 checks)          | ✅ 12/12       | ⚠️ 8/12 — DescribeStream returns incorrect shard info; KEYS_ONLY and stream disable not working            |
| Lambda CRUD (10 checks)               | ✅ 10/10       | ✅ 10/10                                                                                                     |
| Lambda Invoke (4 checks)              | ✅ 4/4         | ✅ 4/4                                                                                                       |
| Lambda HTTP (8 checks)                | ✅ 8/8         | ✅ 8/8                                                                                                       |
| Lambda Warm Pool (3 checks)           | ✅ 3/3         | ✅ 3/3                                                                                                       |
| Lambda Concurrent (3 checks)          | ✅ 3/3         | ✅ 3/3                                                                                                       |
| API Gateway REST (43 checks)          | ✅ 43/43       | ⚠️ 42/43 — `/_api` stage invocation route not mapped                                                       |
| API Gateway v2 / HTTP API (5 checks)  | ✅ 5/5         | ❌ 0/5 — `CreateApi` fails; HTTP APIs not available in community edition                                    |
| S3 Event Notifications (11 checks)    | ✅ 11/11       | ❌ 0/11 — SNS→SQS subscription fails; event notifications not delivered                                    |
| IAM (32 checks)                       | ✅ 32/32       | ⚠️ 27/32 — inline policies (`PutRolePolicy`/`GetRolePolicy`/`ListRolePolicies`) and access key creation differ |
| STS (18 checks)                       | ✅ 18/18       | ⚠️ 6/18 — `AssumeRoleWithWebIdentity`, `AssumeRoleWithSAML`, `GetFederationToken`, `DecodeAuthorizationMessage` not implemented |
| IAM Performance (3 checks)            | ✅ 3/3         | ✅ 3/3                                                                                                       |
| EventBridge (14 checks)               | ✅ 14/14       | ⚠️ 13/14 — `DisableRule` does not immediately halt event delivery                                          |
| CloudWatch Logs (12 checks)           | ✅ 12/12       | ⚠️ 9/12 — tag operations (`TagLogGroup`/`UntagLogGroup`) and `FilterLogEvents` not supported               |
| CloudWatch Metrics (14 checks)        | ✅ 14/14       | ✅ 14/14                                                                                                     |
| Secrets Manager (15 checks)           | ✅ 15/15       | ⚠️ 13/15 — `DescribeSecret` missing version stages in response; `RotateSecret` not supported               |
| KMS (16 checks)                       | ✅ 16/16       | ⚠️ 12/16 — `ReEncrypt` and `Sign`/`Verify` not available                                                  |
| Cognito (8 checks)                    | ✅ 8/8         | ❌ 0/8 — not available in community edition                                                                 |
| Step Functions (7 checks)             | ✅ 7/7         | ⚠️ 6/7 — `DescribeExecution` missing output field until execution fully completes                          |
| Kinesis (15 checks)                   | ✅ 15/15       | ⚠️ 8/15 — `PutRecord`/`GetRecords`, encryption, `SplitShard`, and enhanced fan-out (EFO) not working      |
| ElastiCache (21 checks)               | ✅ 21/21       | ❌ Not available in community edition                                                                       |
| RDS (50 checks)                       | ✅ 50/50       | ❌ Not available in community edition                                                                       |
| **Total**                             | ✅ **408/408** | ⚠️ **305/383 (80%) on overlapping tests; many services unavailable**                                       |

### Performance

|                                      | Floci Native  | LocalStack 4.14.0 | Delta              |
|--------------------------------------|---------------|-------------------|--------------------|
| Startup time                         | **~24 ms**    | ~3.3 s            | 138× faster        |
| Idle memory                          | **~13 MiB**   | ~143 MiB          | −91%               |
| Memory after full test run           | **~80 MiB**   | ~827 MiB          | −90%               |
| Lambda warm latency (avg)            | **2 ms**      | 10 ms             | 5× faster          |
| Lambda warm latency (max)            | **6 ms**      | 68 ms             | 11.3× faster       |
| Lambda throughput (10k / 10 threads) | **289 req/s** | 120 req/s         | 2.4× more req/s    |
| Lambda failures (10k calls)          | **0**         | 0                 | —                  |

### Feature Coverage

| Feature                                                        | Floci          | LocalStack Community                                       |
|----------------------------------------------------------------|----------------|------------------------------------------------------------|
| Auth token required                                            | No             | Yes (since March 2026)                                     |
| CI/CD support                                                  | Unlimited      | Requires paid plan                                         |
| Security updates                                               | Yes            | No (community edition frozen)                              |
| SSM, SQS, SNS, S3, DynamoDB                                    | ✅ All included | ✅ All included                                             |
| S3 Object Lock (COMPLIANCE, GOVERNANCE, legal holds)           | ✅              | ⚠️ Partial — enforcement not blocking deletes              |
| DynamoDB Streams                                               | ✅              | ⚠️ Partial — DescribeStream and KEYS_ONLY mode broken      |
| Lambda execution                                               | ✅              | ✅                                                          |
| Lambda Event Source Mapping (SQS, Kinesis, DynamoDB Streams)   | ✅              | ⚠️ Partial                                                 |
| Lambda Function URLs                                           | ✅              | ⚠️ Partial                                                 |
| API Gateway REST (stage execution)                             | ✅              | ⚠️ Partial — `/_api` route not mapped                      |
| API Gateway v2 / HTTP API                                      | ✅              | ❌ Not available                                            |
| S3 Event Notifications → SNS/SQS                               | ✅              | ❌ SNS subscription fails                                   |
| IAM (full — users, roles, policies, groups, instance profiles) | ✅              | ⚠️ Partial — inline policies differ                        |
| STS (7 operations incl. SAML, WebIdentity, federation)         | ✅              | ⚠️ Partial — 4 of 7 operations missing                     |
| Cognito (user pools, auth flows, JWT well-known endpoints)     | ✅              | ❌ Not available                                            |
| KMS (encrypt, decrypt, sign, verify, data keys)                | ✅              | ⚠️ Partial — ReEncrypt, Sign, Verify not working           |
| Kinesis (streams, shards, enhanced fan-out)                    | ✅              | ⚠️ Partial — PutRecord/GetRecords and EFO not working      |
| Secrets Manager (versioning, resource policies)                | ✅              | ⚠️ Partial — RotateSecret missing                          |
| CloudFormation (stacks, change sets, resource provisioning)    | ✅              | ⚠️ Partial — basic operations only                         |
| Step Functions (ASL executor, executions, task tokens)         | ✅              | ⚠️ Partial — DescribeExecution missing output              |
| ElastiCache (Redis + IAM auth + Lettuce)                       | ✅              | ❌ Not available                                            |
| RDS (PostgreSQL + MySQL + IAM auth + JDBC)                     | ✅              | ❌ Not available                                            |
| EventBridge (rules, targets, event delivery)                   | ✅              | ⚠️ Partial — DisableRule timing issue                      |
| CloudWatch Logs (groups, streams, filter, query)               | ✅              | ⚠️ Partial — tagging and FilterLogEvents missing           |
| CloudWatch Metrics (put, list, statistics, alarms)             | ✅              | ✅ All included                                             |
| License                                                        | MIT            | Restricted                                                 |
| Native binary option                                           | ✅ ~40 MB       | ❌                                                          |
| Docker image size                                              | **~90 MB**     | ~1.0 GB                                                    |
| Startup time                                                   | ~24 ms         | ~3.3 s                                                     |

## License

MIT -- use it however you want.
