package io.github.hectorvent.floci.services.apigatewayv2.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class Integration {
    private String integrationId;
    private String integrationType; // AWS_PROXY, HTTP_PROXY
    private String integrationUri;
    private String payloadFormatVersion; // 1.0, 2.0
    private String integrationMethod;
    private int timeoutInMillis;

    public Integration() {}

    public String getIntegrationId() { return integrationId; }
    public void setIntegrationId(String integrationId) { this.integrationId = integrationId; }

    public String getIntegrationType() { return integrationType; }
    public void setIntegrationType(String integrationType) { this.integrationType = integrationType; }

    public String getIntegrationUri() { return integrationUri; }
    public void setIntegrationUri(String integrationUri) { this.integrationUri = integrationUri; }

    public String getPayloadFormatVersion() { return payloadFormatVersion; }
    public void setPayloadFormatVersion(String payloadFormatVersion) { this.payloadFormatVersion = payloadFormatVersion; }

    public String getIntegrationMethod() { return integrationMethod; }
    public void setIntegrationMethod(String integrationMethod) { this.integrationMethod = integrationMethod; }

    public int getTimeoutInMillis() { return timeoutInMillis; }
    public void setTimeoutInMillis(int timeoutInMillis) { this.timeoutInMillis = timeoutInMillis; }
}
