package io.github.hectorvent.floci.services.apigatewayv2.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class Authorizer {
    private String authorizerId;
    private String authorizerType; // JWT, REQUEST
    private String name;
    private JwtConfiguration jwtConfiguration;
    private List<String> identitySource;

    public Authorizer() {}

    public String getAuthorizerId() { return authorizerId; }
    public void setAuthorizerId(String authorizerId) { this.authorizerId = authorizerId; }

    public String getAuthorizerType() { return authorizerType; }
    public void setAuthorizerType(String authorizerType) { this.authorizerType = authorizerType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public JwtConfiguration getJwtConfiguration() { return jwtConfiguration; }
    public void setJwtConfiguration(JwtConfiguration jwtConfiguration) { this.jwtConfiguration = jwtConfiguration; }

    public List<String> getIdentitySource() { return identitySource; }
    public void setIdentitySource(List<String> identitySource) { this.identitySource = identitySource; }

    @RegisterForReflection
    public record JwtConfiguration(List<String> audience, String issuer) {}
}
