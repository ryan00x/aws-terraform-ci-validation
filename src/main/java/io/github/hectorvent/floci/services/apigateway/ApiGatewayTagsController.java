package io.github.hectorvent.floci.services.apigateway;

import io.github.hectorvent.floci.core.common.AwsException;
import io.github.hectorvent.floci.core.common.RegionResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

/**
 * AWS API Gateway tag endpoints at /tags/{resourceArn}.
 */
@Path("/tags")
@Produces(MediaType.APPLICATION_JSON)
public class ApiGatewayTagsController {

    private final ApiGatewayService service;
    private final RegionResolver regionResolver;
    private final ObjectMapper objectMapper;

    @Inject
    public ApiGatewayTagsController(ApiGatewayService service, RegionResolver regionResolver,
                                    ObjectMapper objectMapper) {
        this.service = service;
        this.regionResolver = regionResolver;
        this.objectMapper = objectMapper;
    }

    @GET
    @Path("/{arn: .*}")
    public Response getTags(@Context HttpHeaders headers, @PathParam("arn") String arn) {
        String region = regionResolver.resolveRegion(headers);
        String apiId = apiIdFromArn(arn);
        Map<String, String> tags = service.getTags(region, apiId);
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode tagsNode = root.putObject("tags");
        tags.forEach(tagsNode::put);
        return Response.ok(root).build();
    }

    @PUT
    @Path("/{arn: .*}")
    public Response tagResource(@Context HttpHeaders headers,
                                @PathParam("arn") String arn,
                                String body) {
        String region = regionResolver.resolveRegion(headers);
        String apiId = apiIdFromArn(arn);
        try {
            JsonNode node = objectMapper.readTree(body);
            Map<String, String> tags = new java.util.HashMap<>();
            node.path("tags").fields().forEachRemaining(e -> tags.put(e.getKey(), e.getValue().asText()));
            service.tagResource(region, apiId, tags);
            return Response.noContent().build();
        } catch (AwsException e) {
            throw e;
        } catch (Exception e) {
            throw new AwsException("BadRequestException", e.getMessage(), 400);
        }
    }

    @DELETE
    @Path("/{arn: .*}")
    public Response untagResource(@Context HttpHeaders headers,
                                  @PathParam("arn") String arn,
                                  @QueryParam("tagKeys") List<String> tagKeys) {
        String region = regionResolver.resolveRegion(headers);
        String apiId = apiIdFromArn(arn);
        service.untagResource(region, apiId, tagKeys);
        return Response.noContent().build();
    }

    /**
     * Extracts apiId from ARN: arn:aws:apigateway:region::/restapis/{apiId}
     */
    private String apiIdFromArn(String arn) {
        String[] parts = arn.split("/restapis/");
        if (parts.length < 2) {
            throw new AwsException("BadRequestException", "Invalid resource ARN: " + arn, 400);
        }
        return parts[1].split("/")[0];
    }
}
