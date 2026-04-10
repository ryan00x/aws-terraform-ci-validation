package com.floci.test;

import org.junit.jupiter.api.*;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.appconfig.AppConfigClient;
import software.amazon.awssdk.services.appconfig.model.*;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AppConfig")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AppConfigTest {

    private static AppConfigClient appConfig;
    private static AppConfigDataClient appConfigData;

    private static String applicationId;
    private static String environmentId;
    private static String configurationProfileId;
    private static String deploymentStrategyId;
    private static String configurationToken;

    @BeforeAll
    static void setup() {
        appConfig = TestFixtures.appConfigClient();
        appConfigData = TestFixtures.appConfigDataClient();
    }

    @AfterAll
    static void cleanup() {
        if (appConfig != null) {
            appConfig.close();
        }
        if (appConfigData != null) {
            appConfigData.close();
        }
    }

    @Test
    @Order(1)
    void createApplication() {
        CreateApplicationResponse response = appConfig.createApplication(CreateApplicationRequest.builder()
                .name(TestFixtures.uniqueName("app"))
                .description("Test Application")
                .build());

        applicationId = response.id();
        assertThat(applicationId).isNotNull();
    }

    @Test
    @Order(2)
    void createEnvironment() {
        CreateEnvironmentResponse response = appConfig.createEnvironment(CreateEnvironmentRequest.builder()
                .applicationId(applicationId)
                .name("dev")
                .build());

        environmentId = response.id();
        assertThat(environmentId).isNotNull();
    }

    @Test
    @Order(3)
    void createConfigurationProfile() {
        CreateConfigurationProfileResponse response = appConfig.createConfigurationProfile(CreateConfigurationProfileRequest.builder()
                .applicationId(applicationId)
                .name("main-config")
                .locationUri("hosted")
                .type("AWS.Freeform")
                .build());

        configurationProfileId = response.id();
        assertThat(configurationProfileId).isNotNull();
    }

    @Test
    @Order(4)
    void createHostedConfigurationVersion() {
        CreateHostedConfigurationVersionResponse response = appConfig.createHostedConfigurationVersion(
                CreateHostedConfigurationVersionRequest.builder()
                        .applicationId(applicationId)
                        .configurationProfileId(configurationProfileId)
                        .content(SdkBytes.fromString("{\"key\": \"value\"}", StandardCharsets.UTF_8))
                        .contentType("application/json")
                        .build());

        assertThat(response.versionNumber()).isEqualTo(1);
    }

    @Test
    @Order(5)
    void createDeploymentStrategy() {
        CreateDeploymentStrategyResponse response = appConfig.createDeploymentStrategy(CreateDeploymentStrategyRequest.builder()
                .name("immediate")
                .deploymentDurationInMinutes(0)
                .finalBakeTimeInMinutes(0)
                .growthFactor(100.0f)
                .build());

        deploymentStrategyId = response.id();
        assertThat(deploymentStrategyId).isNotNull();
    }

    @Test
    @Order(6)
    void startDeployment() {
        StartDeploymentResponse response = appConfig.startDeployment(StartDeploymentRequest.builder()
                .applicationId(applicationId)
                .environmentId(environmentId)
                .configurationProfileId(configurationProfileId)
                .configurationVersion("1")
                .deploymentStrategyId(deploymentStrategyId)
                .build());

        assertThat(response.deploymentNumber()).isNotNull();
    }

    @Test
    @Order(7)
    void startConfigurationSession() {
        var response = appConfigData.startConfigurationSession(StartConfigurationSessionRequest.builder()
                .applicationIdentifier(applicationId)
                .environmentIdentifier(environmentId)
                .configurationProfileIdentifier(configurationProfileId)
                .build());

        configurationToken = response.initialConfigurationToken();
        assertThat(configurationToken).isNotNull();
    }

    @Test
    @Order(8)
    void getLatestConfiguration() {
        GetLatestConfigurationResponse response = appConfigData.getLatestConfiguration(GetLatestConfigurationRequest.builder()
                .configurationToken(configurationToken)
                .build());

        assertThat(response.configuration().asString(StandardCharsets.UTF_8)).isEqualTo("{\"key\": \"value\"}");
        assertThat(response.contentType()).startsWith("application/json");
        assertThat(response.versionLabel()).isEqualTo("1");
        assertThat(response.nextPollConfigurationToken()).isNotNull();
    }
}
