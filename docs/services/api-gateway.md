# API Gateway

Floci supports both API Gateway v1 (REST APIs) and API Gateway v2 (HTTP APIs).

## API Gateway v1 (REST APIs) {#v1}

**Protocol:** REST JSON
**Endpoint:** `http://localhost:4566/restapis/...`

### Supported Operations

| Category | Operations |
|---|---|
| **APIs** | CreateRestApi, GetRestApi, GetRestApis, UpdateRestApi, DeleteRestApi |
| **Resources** | CreateResource, GetResource, GetResources, UpdateResource, DeleteResource |
| **Methods** | PutMethod, GetMethod, DeleteMethod |
| **Method Responses** | PutMethodResponse, GetMethodResponse, DeleteMethodResponse |
| **Integrations** | PutIntegration, GetIntegration, DeleteIntegration |
| **Integration Responses** | PutIntegrationResponse, GetIntegrationResponse, DeleteIntegrationResponse |
| **Deployments** | CreateDeployment, GetDeployment, GetDeployments |
| **Stages** | CreateStage, GetStage, GetStages, UpdateStage, DeleteStage |
| **API Keys** | CreateApiKey, GetApiKey, GetApiKeys, UpdateApiKey, DeleteApiKey |
| **Usage Plans** | CreateUsagePlan, GetUsagePlan, GetUsagePlans, UpdateUsagePlan, DeleteUsagePlan |
| **Base Path Mappings** | CreateBasePathMapping, GetBasePathMapping, DeleteBasePathMapping |

### Examples

```bash
export AWS_ENDPOINT=http://localhost:4566

# Create a REST API
API_ID=$(aws apigateway create-rest-api \
  --name "My API" \
  --query id --output text \
  --endpoint-url $AWS_ENDPOINT)

# Get the root resource
ROOT_ID=$(aws apigateway get-resources \
  --rest-api-id $API_ID \
  --query 'items[?path==`/`].id' --output text \
  --endpoint-url $AWS_ENDPOINT)

# Create a resource
RESOURCE_ID=$(aws apigateway create-resource \
  --rest-api-id $API_ID \
  --parent-id $ROOT_ID \
  --path-part users \
  --query id --output text \
  --endpoint-url $AWS_ENDPOINT)

# Add a GET method
aws apigateway put-method \
  --rest-api-id $API_ID \
  --resource-id $RESOURCE_ID \
  --http-method GET \
  --authorization-type NONE \
  --endpoint-url $AWS_ENDPOINT

# Add a Lambda integration
aws apigateway put-integration \
  --rest-api-id $API_ID \
  --resource-id $RESOURCE_ID \
  --http-method GET \
  --type AWS_PROXY \
  --integration-http-method POST \
  --uri "arn:aws:apigateway:us-east-1:lambda:path/2015-03-31/functions/arn:aws:lambda:us-east-1:000000000000:function:my-function/invocations" \
  --endpoint-url $AWS_ENDPOINT

# Deploy to a stage
aws apigateway create-deployment \
  --rest-api-id $API_ID \
  --stage-name dev \
  --endpoint-url $AWS_ENDPOINT

# Call the deployed API
curl http://localhost:4566/restapis/$API_ID/dev/_user_request_/users
```

---

## API Gateway v2 (HTTP APIs) {#v2}

**Protocol:** REST JSON
**Endpoint:** `http://localhost:4566/v2/apis/...`

### Supported Operations

| Category | Operations |
|---|---|
| **APIs** | CreateApi, GetApi, GetApis, DeleteApi |
| **Routes** | CreateRoute, GetRoute, GetRoutes, DeleteRoute |
| **Integrations** | CreateIntegration, GetIntegration, GetIntegrations |
| **Authorizers** | CreateAuthorizer, GetAuthorizer, GetAuthorizers, DeleteAuthorizer |
| **Stages** | CreateStage, GetStage, GetStages, DeleteStage |
| **Deployments** | CreateDeployment, GetDeployments |

### Examples

```bash
export AWS_ENDPOINT=http://localhost:4566

# Create an HTTP API
API_ID=$(aws apigatewayv2 create-api \
  --name "My HTTP API" \
  --protocol-type HTTP \
  --query ApiId --output text \
  --endpoint-url $AWS_ENDPOINT)

# Create a Lambda integration
INTEGRATION_ID=$(aws apigatewayv2 create-integration \
  --api-id $API_ID \
  --integration-type AWS_PROXY \
  --integration-uri "arn:aws:lambda:us-east-1:000000000000:function:my-function" \
  --payload-format-version 2.0 \
  --query IntegrationId --output text \
  --endpoint-url $AWS_ENDPOINT)

# Create a route
aws apigatewayv2 create-route \
  --api-id $API_ID \
  --route-key "GET /users" \
  --target "integrations/$INTEGRATION_ID" \
  --endpoint-url $AWS_ENDPOINT

# Deploy
aws apigatewayv2 create-stage \
  --api-id $API_ID \
  --stage-name dev \
  --auto-deploy \
  --endpoint-url $AWS_ENDPOINT
```