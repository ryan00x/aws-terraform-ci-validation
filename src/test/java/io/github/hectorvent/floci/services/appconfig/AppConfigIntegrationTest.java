package io.github.hectorvent.floci.services.appconfig;

import io.github.hectorvent.floci.testing.RestAssuredJsonUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AppConfigIntegrationTest {

    private static String appId;
    private static String envId;
    private static String profileId;
    private static String strategyId;
    private static String configToken;

    @BeforeAll
    static void setup() {
        RestAssuredJsonUtils.configureAwsContentTypes();
    }

    @Test @Order(1)
    void createApplication() {
        appId = given()
                .contentType(ContentType.JSON)
                .body("{\"Name\": \"test-app\", \"Description\": \"Test App\"}")
                .when().post("/applications")
                .then()
                .statusCode(201)
                .body("Name", equalTo("test-app"))
                .extract().path("Id");
    }

    @Test @Order(2)
    void createEnvironment() {
        envId = given()
                .contentType(ContentType.JSON)
                .body("{\"Name\": \"test-env\"}")
                .when().post("/applications/" + appId + "/environments")
                .then()
                .statusCode(201)
                .body("Name", equalTo("test-env"))
                .extract().path("Id");
    }

    @Test @Order(3)
    void createConfigurationProfile() {
        profileId = given()
                .contentType(ContentType.JSON)
                .body("{\"Name\": \"test-profile\", \"LocationUri\": \"hosted\", \"Type\": \"AWS.Freeform\"}")
                .when().post("/applications/" + appId + "/configurationprofiles")
                .then()
                .statusCode(201)
                .body("Name", equalTo("test-profile"))
                .extract().path("Id");
    }

    @Test @Order(4)
    void createHostedConfigurationVersion() {
        given()
                .header("Content-Type", "application/json")
                .header("Description", "v1")
                .body("{\"foo\": \"bar\"}".getBytes())
                .when().post("/applications/" + appId + "/configurationprofiles/" + profileId + "/hostedconfigurationversions")
                .then()
                .statusCode(201)
                .header("Version-Number", equalTo("1"));
    }

    @Test @Order(5)
    void createDeploymentStrategy() {
        strategyId = given()
                .contentType(ContentType.JSON)
                .body("{\"Name\": \"immediate\", \"DeploymentDurationInMinutes\": 0, \"GrowthFactor\": 100, \"FinalBakeTimeInMinutes\": 0}")
                .when().post("/deploymentstrategies")
                .then()
                .statusCode(201)
                .body("Name", equalTo("immediate"))
                .extract().path("Id");
    }

    @Test @Order(6)
    void startDeployment() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"ConfigurationProfileId\": \"" + profileId + "\", \"ConfigurationVersion\": \"1\", \"DeploymentStrategyId\": \"" + strategyId + "\"}")
                .when().post("/applications/" + appId + "/environments/" + envId + "/deployments")
                .then()
                .statusCode(201)
                .body("State", equalTo("COMPLETE"));
    }

    @Test @Order(7)
    void startConfigurationSession() {
        configToken = given()
                .contentType(ContentType.JSON)
                .body("{\"ApplicationIdentifier\": \"" + appId + "\", \"EnvironmentIdentifier\": \"" + envId + "\", \"ConfigurationProfileIdentifier\": \"" + profileId + "\"}")
                .when().post("/configurationsessions")
                .then()
                .statusCode(201)
                .body("InitialConfigurationToken", notNullValue())
                .extract().path("InitialConfigurationToken");
    }

    @Test @Order(8)
    void getLatestConfiguration() {
        given()
                .queryParam("configuration_token", configToken)
                .when().get("/configuration")
                .then()
                .statusCode(200)
                .header("Content-Type", startsWith("application/json"))
                .header("Version-Label", equalTo("1"))
                .header("Next-Poll-Configuration-Token", notNullValue())
                .body("foo", equalTo("bar"));
    }
}
