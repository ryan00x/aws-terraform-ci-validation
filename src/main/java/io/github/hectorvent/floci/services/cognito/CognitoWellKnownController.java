package io.github.hectorvent.floci.services.cognito;

import io.github.hectorvent.floci.services.cognito.model.UserPool;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Exposes Cognito well-known endpoints.
 * The JWKS endpoint allows downstream services to verify JWTs issued by Floci Cognito pools.
 * Path mirrors real AWS: /{userPoolId}/.well-known/jwks.json
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class CognitoWellKnownController {

    private final CognitoService cognitoService;

    @Inject
    public CognitoWellKnownController(CognitoService cognitoService) {
        this.cognitoService = cognitoService;
    }

    @GET
    @Path("/{poolId}/.well-known/jwks.json")
    public Response getJwks(@PathParam("poolId") String poolId) {
        UserPool pool = cognitoService.describeUserPool(poolId);
        String kid = pool.getId();
        // Encode signing secret bytes as Base64URL (no padding) for the JWK "k" parameter
        byte[] secretBytes = pool.getSigningSecret().getBytes(StandardCharsets.UTF_8);
        String k = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);

        String body = """
                {"keys":[{"kty":"oct","kid":"%s","alg":"HS256","k":"%s","use":"sig"}]}
                """.formatted(kid, k).strip();
        return Response.ok(body).build();
    }
}
