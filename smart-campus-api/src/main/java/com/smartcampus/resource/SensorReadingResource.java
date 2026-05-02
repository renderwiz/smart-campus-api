package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

/**
 * Part 4 - Sub-Resource for Sensor Readings
 * Handles /api/v1/sensors/{sensorId}/readings
 *
 * This class is instantiated by SensorResource's sub-resource locator,
 * keeping reading logic cleanly separated from sensor management.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String    sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // ----------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}/readings
    // ----------------------------------------------------------------
    @GET
    public Response getReadings() {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                        "status",  404,
                        "error",   "Not Found",
                        "message", "Sensor '" + sensorId + "' does not exist."
                    )).build();
        }
        List<SensorReading> list = store.getReadings(sensorId);
        return Response.ok(list).build();
    }

    // ----------------------------------------------------------------
    // POST /api/v1/sensors/{sensorId}/readings
    // Side effect: updates Sensor.currentValue with the new reading.
    // Throws SensorUnavailableException if sensor status is MAINTENANCE.
    // ----------------------------------------------------------------
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                        "status",  404,
                        "error",   "Not Found",
                        "message", "Sensor '" + sensorId + "' does not exist."
                    )).build();
        }
        // Block readings for sensors under maintenance
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId);
        }
        // Auto-generate id and timestamp if not provided
        if (reading.getId() == null) {
            reading = new SensorReading(reading.getValue());
        } else {
            if (reading.getTimestamp() == 0) reading.setTimestamp(System.currentTimeMillis());
        }
        store.addReading(sensorId, reading);
        // Side effect: keep currentValue in sync with latest reading
        sensor.setCurrentValue(reading.getValue());
        return Response.status(Response.Status.CREATED).entity(reading).build();
    }

    // ----------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}/readings/{readingId}
    // ----------------------------------------------------------------
    @GET
    @Path("/{readingId}")
    public Response getReading(@PathParam("readingId") String readingId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("status", 404, "error", "Not Found",
                        "message", "Sensor '" + sensorId + "' does not exist.")).build();
        }
        return store.getReadings(sensorId).stream()
                .filter(r -> r.getId().equals(readingId))
                .findFirst()
                .map(r -> Response.ok(r).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("status", 404, "error", "Not Found",
                            "message", "Reading '" + readingId + "' not found.")).build());
    }
}
