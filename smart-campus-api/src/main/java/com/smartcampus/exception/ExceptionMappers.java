package com.smartcampus.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// ============================================================
//  Part 5.1 - 409 Conflict: Room has active sensors
// ============================================================
@Provider
class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {
    @Override
    public Response toResponse(RoomNotEmptyException e) {
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                    "status",  409,
                    "error",   "Conflict",
                    "message", e.getMessage(),
                    "roomId",  e.getRoomId(),
                    "activeSensors", e.getSensorCount()
                )).build();
    }
}

// ============================================================
//  Part 5.2 - 422 Unprocessable Entity: invalid foreign-key ref
// ============================================================
@Provider
class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    @Override
    public Response toResponse(LinkedResourceNotFoundException e) {
        return Response.status(422)          // Jakarta doesn't have a 422 constant in older builds
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                    "status",       422,
                    "error",        "Unprocessable Entity",
                    "message",      e.getMessage(),
                    "resourceType", e.getResourceType(),
                    "resourceId",   String.valueOf(e.getResourceId())
                )).build();
    }
}

// ============================================================
//  Part 5.3 - 403 Forbidden: sensor is in MAINTENANCE
// ============================================================
@Provider
class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {
    @Override
    public Response toResponse(SensorUnavailableException e) {
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                    "status",   403,
                    "error",    "Forbidden",
                    "message",  e.getMessage(),
                    "sensorId", e.getSensorId()
                )).build();
    }
}

// ============================================================
//  Part 5.4 - 500 Global safety net for unexpected errors
// ============================================================
@Provider
class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable t) {
        // Log internally but NEVER expose stack trace to client (security risk)
        LOGGER.log(Level.SEVERE, "Unexpected error", t);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                    "status",  500,
                    "error",   "Internal Server Error",
                    "message", "An unexpected error occurred. Please contact support."
                )).build();
    }
}
