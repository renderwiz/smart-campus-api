package com.smartcampus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part 1 - Discovery Endpoint
 * GET /api/v1
 * Returns API metadata including version info, contact, and resource links (HATEOAS).
 */
@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response discover() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("api",     "Smart Campus Sensor & Room Management API");
        response.put("version", "v1.0.0");
        response.put("status",  "UP");
        response.put("contact", Map.of(
            "name",  "Campus Facilities Admin",
            "email", "admin@smartcampus.ac.uk"
        ));

        // HATEOAS - links to primary resource collections
        response.put("resources", Map.of(
            "rooms",   "/api/v1/rooms",
            "sensors", "/api/v1/sensors"
        ));

        response.put("links", new Object[]{
            linkObj("self",    "GET", "/api/v1"),
            linkObj("rooms",   "GET", "/api/v1/rooms"),
            linkObj("sensors", "GET", "/api/v1/sensors")
        });

        return Response.ok(response).build();
    }

    private Map<String, String> linkObj(String rel, String method, String href) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("rel",    rel);
        m.put("method", method);
        m.put("href",   href);
        return m;
    }
}
