package io.github.hectorvent.floci.services.s3;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;
import java.net.URI;

@Provider
@PreMatching
public class S3VirtualHostFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String host = requestContext.getHeaderString("Host");
        if (host == null) return;

        // Pattern: bucket.localhost:4566 or bucket.s3.amazonaws.com or bucket.s3.region.amazonaws.com
        if (host.contains(".localhost") || host.contains(".s3.")) {
            int dotIndex = host.indexOf('.');
            String bucket = host.substring(0, dotIndex);
            
            // Check if it's not just "s3" or "localhost"
            if (bucket.equalsIgnoreCase("s3") || bucket.equalsIgnoreCase("localhost")) {
                return;
            }

            URI uri = requestContext.getUriInfo().getRequestUri();
            String path = uri.getRawPath();
            
            // Rewrite path from /key to /bucket/key
            String newPath = "/" + bucket + (path.startsWith("/") ? "" : "/") + path;
            
            URI newUri = UriBuilder.fromUri(uri)
                    .replacePath(newPath)
                    .build();
            
            requestContext.setRequestUri(newUri);
        }
    }
}
