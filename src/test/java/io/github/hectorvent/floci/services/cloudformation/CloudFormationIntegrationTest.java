package io.github.hectorvent.floci.services.cloudformation;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class CloudFormationIntegrationTest {

    @Test
    void createStack_withS3AndSqs() {
        String template = """
            {
              "Resources": {
                "MyBucket": {
                  "Type": "AWS::S3::Bucket",
                  "Properties": {
                    "BucketName": "cf-test-bucket"
                  }
                },
                "MyQueue": {
                  "Type": "AWS::SQS::Queue",
                  "Properties": {
                    "QueueName": "cf-test-queue"
                  }
                }
              }
            }
            """;

        // 1. Create Stack
        given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("Action", "CreateStack")
            .formParam("StackName", "test-stack")
            .formParam("TemplateBody", template)
        .when()
            .post("/")
        .then()
            .statusCode(200)
            .body(containsString("<StackId>"));

        // 2. Verify S3 Bucket exists
        given()
            .header("Host", "cf-test-bucket.localhost")
        .when()
            .get("/")
        .then()
            .statusCode(200);

        // 3. Verify SQS Queue exists
        given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("Action", "GetQueueUrl")
            .formParam("QueueName", "cf-test-queue")
        .when()
            .post("/")
        .then()
            .statusCode(200)
            .body(containsString("cf-test-queue"));
        
        // 4. Describe Stacks
        given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("Action", "DescribeStacks")
            .formParam("StackName", "test-stack")
        .when()
            .post("/")
        .then()
            .statusCode(200)
            .body(containsString("<StackName>test-stack</StackName>"))
            .body(containsString("<StackStatus>CREATE_COMPLETE</StackStatus>"));
    }
}
