package io.github.hectorvent.floci.services.cognito.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPoolClient {
    private String clientId;
    private String userPoolId;
    private String clientName;
    private long creationDate;
    private long lastModifiedDate;

    public UserPoolClient() {
        long now = System.currentTimeMillis() / 1000L;
        this.creationDate = now;
        this.lastModifiedDate = now;
    }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getUserPoolId() { return userPoolId; }
    public void setUserPoolId(String userPoolId) { this.userPoolId = userPoolId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public long getCreationDate() { return creationDate; }
    public void setCreationDate(long creationDate) { this.creationDate = creationDate; }

    public long getLastModifiedDate() { return lastModifiedDate; }
    public void setLastModifiedDate(long lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }
}
