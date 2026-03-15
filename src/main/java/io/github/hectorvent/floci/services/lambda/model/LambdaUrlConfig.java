package io.github.hectorvent.floci.services.lambda.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class LambdaUrlConfig {

    private String functionUrl;
    private String authType; // NONE or AWS_IAM
    private String creationTime;
    private String lastModifiedTime;
    private Cors cors;

    public LambdaUrlConfig() {}

    public String getFunctionUrl() { return functionUrl; }
    public void setFunctionUrl(String functionUrl) { this.functionUrl = functionUrl; }

    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }

    public String getCreationTime() { return creationTime; }
    public void setCreationTime(String creationTime) { this.creationTime = creationTime; }

    public String getLastModifiedTime() { return lastModifiedTime; }
    public void setLastModifiedTime(String lastModifiedTime) { this.lastModifiedTime = lastModifiedTime; }

    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }

    @RegisterForReflection
    public static class Cors {
        private boolean allowCredentials;
        private String[] allowHeaders;
        private String[] allowMethods;
        private String[] allowOrigins;
        private String[] exposeHeaders;
        private Integer maxAge;

        public boolean isAllowCredentials() { return allowCredentials; }
        public void setAllowCredentials(boolean allowCredentials) { this.allowCredentials = allowCredentials; }

        public String[] getAllowHeaders() { return allowHeaders; }
        public void setAllowHeaders(String[] allowHeaders) { this.allowHeaders = allowHeaders; }

        public String[] getAllowMethods() { return allowMethods; }
        public void setAllowMethods(String[] allowMethods) { this.allowMethods = allowMethods; }

        public String[] getAllowOrigins() { return allowOrigins; }
        public void setAllowOrigins(String[] allowOrigins) { this.allowOrigins = allowOrigins; }

        public String[] getExposeHeaders() { return exposeHeaders; }
        public void setExposeHeaders(String[] exposeHeaders) { this.exposeHeaders = exposeHeaders; }

        public Integer getMaxAge() { return maxAge; }
        public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }
    }
}
