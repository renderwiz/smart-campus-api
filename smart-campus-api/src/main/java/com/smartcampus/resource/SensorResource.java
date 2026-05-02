package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Part 3 - Sensor Operations
 * Manages /api/v1/sensors
 * Also provides sub-resource locator to SensorReadingResource.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    // ----------------------------------------------------------------
    // GET /api/v1/sensors  (optional ?type=CO2 filter)
    // ----------------------------------------------------------------
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> list = new ArrayList<>(store.getSensors().values());
        if (type != null && !type.isBlank()) {
            list = list.stream()
                       .filter(s -> s.getType().equalsIgnoreCase(type))
                       .collect(Collectors.toList());
        }
        return Response.ok(list).build();
    }

    // ----------------------------------------------------------------
    // POST /api/v1/sensors  - register a new sensor
    // Validates that the referenced roomId actually exists.
    // ----------------------------------------------------------------
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                        "status",  400,
                        "error",   "Bad Request",
                        "message", "Sensor id is required."
                    )).build();
        }
        if (store.getSensor(sensor.getId()) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of(
                        "status",  409,
                        "error",   "Conflict",
                        "message", "Sensor with id '" + sensor.getId() + "' already exists."
                    )).build();
        }
        // Validate that the roomId exists
        if (sensor.getRoomId() == null || store.getRoom(sensor.getRoomId()) == null) {
            throw new LinkedResourceNotFoundException("Room", sensor.getRoomId());
        }
        store.putSensor(sensor.getId(), sensor);
        // Link sensor back to the room
        Room room = store.getRoom(sensor.getRoomId());
        room.getSensorIds().add(sensor.getId());
        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    // ----------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}
    // ----------------------------------------------------------------
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                        "status",  404,
                        "error",   "Not Found",
                        "message", "Sensor '" + sensorId + "' does not exist."
                    )).build();
        }
        return Response.ok(sensor).build();
    }

    // ----------------------------------------------------------------
    // DELETE /api/v1/sensors/{sensorId}
    // ----------------------------------------------------------------
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                        "status",  404,
                        "error",   "Not Found",
                        "message", "Sensor '" + sensorId + "' does not exist."
                    )).build();
        }
        // Remove sensor reference from its room
        Room room = store.getRoom(sensor.getRoomId());
        if (room != null) {
            room.getSensorIds().remove(sensorId);
        }
        store.deleteSensor(sensorId);
        return Response.noContent().build();
    }

    // ----------------------------------------------------------------
    // Sub-Resource Locator: /api/v1/sensors/{sensorId}/readings
    // Delegates to SensorReadingResource for all reading operations.
    // ----------------------------------------------------------------
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
